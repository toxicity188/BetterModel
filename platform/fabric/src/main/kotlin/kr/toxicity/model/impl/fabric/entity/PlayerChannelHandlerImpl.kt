/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.entity

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.entity.BasePlayer
import kr.toxicity.model.api.fabric.BetterModelFabric
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.PlayerChannelHandler
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.tracker.TrackerUpdateAction
import kr.toxicity.model.impl.fabric.network.*
import kr.toxicity.model.impl.fabric.wrap
import kr.toxicity.model.mixin.DisplayAccessor
import kr.toxicity.model.mixin.EntityAccessor
import kr.toxicity.model.util.CONFIG
import kr.toxicity.model.util.PLATFORM
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.network.ServerPlayerConnection
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import java.util.stream.IntStream

class PlayerChannelHandlerImpl(
    private val connection: ServerPlayerConnection
) : PlayerChannelHandler, ChannelDuplexHandler() {
    private val player get() = connection.player
    private val uuid = player.uuid

    private val basePlayer = PLATFORM.nms().adapt(connection.wrap())

    init {
        val pipeline = connection.player.connection.connection.channel.pipeline()
        pipeline.addBefore(pipeline.first { it.value is Connection }.key, INJECT_NAME, this)
    }

    override fun close() {
        val channel = connection.player.connection.connection.channel
        channel.eventLoop().submit {
            channel.pipeline().remove(INJECT_NAME)
        }
    }

    override fun base(): BasePlayer = basePlayer

    private val playerModel get() = connection.player.id.toRegistry()

    private fun getEntity(id: Int, level: ServerLevel) = level.getEntity(id)

    private fun getPlayerEntity(id: Int) = getEntity(id, connection.player.level())

    private fun Entity.toRegistry() = BetterModel.registryOrNull(uuid)

    private inline fun Int.toRegistry(ifHitBox: (Entity) -> Unit = {}) =
        (EntityTrackerRegistry.registry(this) ?: getPlayerEntity(this)?.let {
            if (it is HitBox) ifHitBox(it)
            it.toRegistry()
        })?.takeIf {
            it.isSpawned(player.uuid)
        }

    override fun sendEntityData(registry: EntityTrackerRegistry) {
        val handle = registry.entity().handle() as? Entity ?: return
        val list = bundlerOf(
            ClientboundSetPassengersPacket(handle)
        )

        handle.entityData.pack(
            valueFilter = { it.id == EntityAccessor.`bettermodel$getDataSharedFlagsId`().id }
        )?.let {
            list.add(ClientboundSetEntityDataPacket(handle.id, it))
        }

        if (handle is LivingEntity) handle.toEquipmentPacket()?.let {
            list.add(it)
        }

        list.send(connection.wrap())
    }

    private fun Entity.toFakeAddPacket() = ClientboundAddEntityPacket(
        id,
        uuid,
        x, y, z,
        xRot, yRot,
        EntityType.ITEM_DISPLAY,
        0,
        deltaMovement,
        yHeadRot.toDouble()
    )

    private fun <T : ClientGamePacketListener> Packet<in T>.handle(): Packet<in T>? {
        when (this) {
            is ClientboundBundlePacket -> {
                return if ((this as BetterModelBundlePacket).`bettermodel$isBetterModelPacket`()) this else ClientboundBundlePacket(subPackets().mapNotNull {
                    it.handle()
                })
            }

            is ClientboundAddEntityPacket -> {
                val entity = getPlayerEntity(id) ?: return this
                if (entity is HitBox) return entity.toFakeAddPacket()
                val wrap = entity.wrap()
                BetterModel.registry(wrap).ifPresent {
                    wrap.taskLater(1) {
                        it.spawn(connection.wrap())
                    }
                }
            }

            is ClientboundRemoveEntitiesPacket -> {
                entityIds
                    .asSequence()
                    .mapNotNull map@{
                        it.toRegistry {
                            return@map null
                        }
                    }
                    .forEach {
                        it.remove()
                    }
            }

            is ClientboundSetPassengersPacket -> {
                vehicle.toRegistry()?.let { registry ->
                    return registry.mountPacket(
                        entity = registry.entity().handle() as? Entity ?: return this,
                        passengerIds = IntStream.of(*passengers)
                    )
                }
            }

            is ClientboundUpdateAttributesPacket if getPlayerEntity(entityId) is HitBox -> return null
            is ClientboundSetEntityDataPacket -> id.toRegistry {
                return ClientboundSetEntityDataPacket(id, hitBoxData)
            }?.let { registry ->
                return toRegistryDataPacket(uuid, registry)
            }

            is ClientboundSetEquipmentPacket -> entity.toRegistry {
                return null
            }?.let { registry ->
                if (registry.hideOption(uuid).equipment()) {
                    (registry.entity().handle() as? LivingEntity)
                        ?.toEquipmentPacket { ItemStack.EMPTY }
                        ?.let { packet ->
                            return packet
                        }
                }
            }

            is ClientboundRespawnPacket -> playerModel?.let {
                bundlerOf(it.mountPacket(connection.player)).send(connection.wrap())
            }

            is ClientboundContainerSetSlotPacket if isEquipment(connection.player) && playerModel?.hideOption(uuid)?.equipment() == true -> {
                return ClientboundContainerSetSlotPacket(containerId, stateId, slot, ItemStack.EMPTY)
            }

            is ClientboundContainerSetContentPacket if containerId == 0 && playerModel?.hideOption(uuid)?.equipment() == true -> {
                return ClientboundContainerSetContentPacket(
                    containerId,
                    stateId,
                    items.apply {
                        eachEquipmentSlots { set(it, ItemStack.EMPTY) }
                        set(connection.player.hotbarSlot, ItemStack.EMPTY)
                    },
                    carriedItem
                )
            }
        }
        return this
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        super.write(ctx, if (msg is Packet<*>) msg.handle() ?: return else msg, promise)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        fun EntityTrackerRegistry.updatePlayerLimb() = BetterModel.platform().scheduler().asyncTaskLater(1) {
            if (isClosed) return@asyncTaskLater
            player.containerMenu.sendAllDataToRemote()
            trackers().forEach { tracker ->
                tracker.update(TrackerUpdateAction.itemMapping()) { bone ->
                    !bone.itemMapper.fixed()
                }
            }
        }
        when (msg) {
            is ServerboundSetCarriedItemPacket -> {
                playerModel?.let { registry ->
                    if (!registry.hideOption(uuid).equipment()) return super.channelRead(ctx, msg)
                    if (CONFIG.cancelPlayerModelInventory()) {
                        connection.send(ClientboundSetHeldSlotPacket(player.inventory.selectedSlot))
                        return
                    }
                    registry.updatePlayerLimb()
                }
            }

            is ServerboundPlayerActionPacket -> {
                playerModel?.let { registry ->
                    if (!registry.hideOption(uuid).equipment()) return super.channelRead(ctx, msg)
                    if (CONFIG.cancelPlayerModelInventory()) return
                    registry.updatePlayerLimb()
                }
            }
        }
        super.channelRead(ctx, msg)
    }

    private fun EntityTrackerRegistry.remove() {
        remove(connection.wrap())
    }

    companion object {
        private const val INJECT_NAME = "bettermodel_channel_handler"

        private val hitBoxData by lazy {
            Display.ItemDisplay(
                EntityType.ITEM_DISPLAY,
                (PLATFORM as BetterModelFabric).server().overworld()
            ).run {
                entityData.set(DisplayAccessor.`bettermodel$getDataPosRotInterpolationDurationId`(), 3)
                entityData.nonDefaultValues!!
            }
        }
    }
}
