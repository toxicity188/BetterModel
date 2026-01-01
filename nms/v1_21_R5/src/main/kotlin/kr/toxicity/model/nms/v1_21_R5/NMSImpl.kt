/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_21_R5

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bone.RenderedBone
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox
import kr.toxicity.model.api.entity.BaseBukkitEntity
import kr.toxicity.model.api.entity.BaseBukkitPlayer
import kr.toxicity.model.api.entity.BaseEntity
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.*
import kr.toxicity.model.api.player.PlayerSkinParts
import kr.toxicity.model.api.profile.ModelProfile
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.tracker.TrackerUpdateAction
import kr.toxicity.model.api.util.TransformedItemStack
import net.kyori.adventure.key.Keyed
import net.minecraft.core.component.DataComponents
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.util.ARGB
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.level.entity.LevelEntityGetterAdapter
import net.minecraft.world.level.entity.PersistentEntitySectionManager
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.function.Consumer

class NMSImpl : NMS {

    companion object {
        private const val INJECT_NAME = "bettermodel_channel_handler"

        //Spigot
        private val getGameProfile: (net.minecraft.world.entity.player.Player) -> GameProfile = createAdaptedFieldGetter { it.gameProfile }
        private val getConnection: (ServerCommonPacketListenerImpl) -> Connection = createAdaptedFieldGetter { it.connection }
        private val spigotChunkAccess = ServerLevel::class.java.fields.firstOrNull {
            it.type == PersistentEntitySectionManager::class.java
        }?.apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        private val ServerLevel.levelGetter
            get(): LevelEntityGetter<Entity> {
                return if (BetterModel.IS_PAPER) {
                    `moonrise$getEntityLookup`()
                } else {
                    spigotChunkAccess?.get(this)?.let {
                        (it as PersistentEntitySectionManager<*>).entityGetter as LevelEntityGetter<Entity>
                    } ?: throw RuntimeException("LevelEntityGetter")
                }
            }
        private val getEntityById: (LevelEntityGetter<Entity>, Int) -> Entity? = if (BetterModel.IS_PAPER) { g, i ->
            (g as EntityLookup)[i]
        } else LevelEntityGetterAdapter::class.java.declaredFields.first {
            net.minecraft.world.level.entity.EntityLookup::class.java.isAssignableFrom(it.type)
        }.let {
            it.isAccessible = true
            { e, i ->
                (it[e] as net.minecraft.world.level.entity.EntityLookup<*>).getEntity(i) as? Entity
            }
        }
        private fun Int.toEntity(level: ServerLevel) = getEntityById(level.levelGetter, this)
        //Spigot
        private val hitBoxData by lazy {
            ItemDisplay(EntityType.ITEM_DISPLAY, MinecraftServer.getServer().overworld()).run {
                entityData[Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID] = 3
                entityData.nonDefaultValues!!
            }
        }
    }

    override fun hide(channel: PlayerChannelHandler, registry: EntityTrackerRegistry) {
        val target = registry.entity().handle() as? Entity ?: return
        val list = bundlerOf()
        target.entityData.pack(
            valueFilter = { it.id == SHARED_FLAG }
        )?.let {
            list += ClientboundSetEntityDataPacket(target.id, it).toRegistryDataPacket(channel.uuid(), registry)
        }
        if (target is LivingEntity) {
            val packet = if (registry.hideOption(channel.uuid()).equipment) target.toEmptyEquipmentPacket() else target.toEquipmentPacket()
            packet?.let { list += it }
        }
        list.send(channel.player())
    }

    private fun ClientboundSetEntityDataPacket.toRegistryDataPacket(uuid: UUID, registry: EntityTrackerRegistry) = ClientboundSetEntityDataPacket(id, packedItems().map {
        if (it.id == SHARED_FLAG) SynchedEntityData.DataValue(
            it.id,
            EntityDataSerializers.BYTE,
            registry.entityFlag(uuid, it.value() as Byte)
        ) else it
    })

    inner class PlayerChannelHandlerImpl(
        private val player: CraftPlayer
    ) : PlayerChannelHandler, ChannelDuplexHandler() {
        private val connection = player.handle.connection
        private val uuid = player.uniqueId
        private val base = adapt(player)

        init {
            val pipeline = getConnection(connection).channel.pipeline()
            pipeline.addBefore(pipeline.first { it.value is Connection }.key, INJECT_NAME, this)
        }

        override fun close() {
            val channel = getConnection(connection).channel
            channel.eventLoop().submit {
                channel.pipeline().remove(INJECT_NAME)
            }
        }

        override fun base(): BaseBukkitPlayer = base

        private val playerModel get() = connection.player.id.toRegistry()

        private fun Int.toPlayerEntity() = toEntity(connection.player.level())
        private fun Entity.toRegistry() = BetterModel.registryOrNull(uuid)
        private inline fun Int.toRegistry(
            ifHitBox: (Entity) -> Unit = {}
        ) = (EntityTrackerRegistry.registry(this) ?: toPlayerEntity()?.let {
            if (it is HitBox) ifHitBox(it)
            it.toRegistry()
        })?.takeIf {
            it.isSpawned(player)
        }

        override fun sendEntityData(registry: EntityTrackerRegistry) {
            val handle = registry.entity().handle() as? Entity ?: return
            val list = bundlerOf(
                ClientboundSetPassengersPacket(handle)
            )
            handle.entityData.pack(
                valueFilter = { it.id == SHARED_FLAG }
            )?.let {
                list += ClientboundSetEntityDataPacket(handle.id, it)
            }
            if (handle is LivingEntity) handle.toEquipmentPacket()?.let {
                list += it
            }
            list.send(player)
        }

        private fun <T : ClientGamePacketListener> Packet<in T>.handle(): Packet<in T>? {
            when (this) {
                is ClientboundBundlePacket -> return if (subPackets() is Keyed) this else ClientboundBundlePacket(subPackets().mapNotNull {
                    it.handle()
                })
                is ClientboundAddEntityPacket -> {
                    val entity = id.toPlayerEntity() ?: return this
                    if (entity is HitBox) return entity.toFakeAddPacket()
                    BetterModel.registry(entity.bukkitEntity).ifPresent {
                        BetterModel.plugin().scheduler().taskLater(entity.bukkitEntity, 1) {
                            it.spawn(player)
                        }
                    }
                }
                is ClientboundRemoveEntitiesPacket -> {
                    entityIds
                        .asSequence()
                        .mapNotNull map@ {
                            it.toRegistry {
                                return@map null
                            }
                        }
                        .forEach {
                            it.remove()
                        }
                }
                is ClientboundSetPassengersPacket -> {
                    vehicle.toRegistry()?.let {
                        return it.mountPacket(it.entity().handle() as? Entity ?: return this, array = passengers)
                    }
                }
                is ClientboundUpdateAttributesPacket if entityId.toPlayerEntity() is HitBox -> return null
                is ClientboundSetEntityDataPacket -> id.toRegistry {
                    return ClientboundSetEntityDataPacket(id, hitBoxData)
                }?.let { registry ->
                    return toRegistryDataPacket(uuid, registry)
                }
                is ClientboundSetEquipmentPacket -> entity.toRegistry {
                    return null
                }?.let {
                    if (it.hideOption(uuid).equipment()) (it.entity().handle() as? LivingEntity)?.toEmptyEquipmentPacket()?.let { packet ->
                        return packet
                    }
                }
                is ClientboundRespawnPacket -> playerModel?.let {
                    bundlerOf(it.mountPacket(connection.player)).send(player)
                }
                is ClientboundContainerSetSlotPacket if isEquipment(connection.player) && playerModel?.hideOption(uuid)?.equipment() == true -> {
                    return ClientboundContainerSetSlotPacket(containerId, stateId, slot, EMPTY_ITEM)
                }
                is ClientboundContainerSetContentPacket if containerId == 0 && playerModel?.hideOption(uuid)?.equipment() == true -> {
                    return ClientboundContainerSetContentPacket(
                        containerId,
                        stateId,
                        items.apply {
                            PLAYER_EQUIPMENT_SLOT.forEach { set(it, EMPTY_ITEM) }
                            set(connection.player.hotbarSlot, EMPTY_ITEM)
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
            fun EntityTrackerRegistry.updatePlayerLimb() = BetterModel.plugin().scheduler().asyncTaskLater(1) {
                if (isClosed) return@asyncTaskLater
                player.handle.containerMenu.sendAllDataToRemote()
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
                            connection.send(ClientboundSetHeldSlotPacket(player.inventory.heldItemSlot))
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
            remove(player)
        }
    }

    override fun mount(registry: EntityTrackerRegistry, bundler: PacketBundler) {
        val entity = registry.entity().handle()
        if (entity is Entity) bundler += registry.mountPacket(entity)
    }

    private fun EntityTrackerRegistry.mountPacket(entity: Entity, array: IntArray = entity.passengers.filter {
        EntityTrackerRegistry.registry(it.uuid) == null
    }.map {
        it.id
    }.toIntArray()): ClientboundSetPassengersPacket {
        return useByteBuf { buffer ->
            buffer.writeVarInt(entity.id)
            buffer.writeVarIntArray(displays()
                .mapToInt {
                    (it as ModelDisplayImpl).display.id
                }.toArray() + array)
            ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer)
        }
    }

    override fun inject(player: Player): PlayerChannelHandlerImpl = PlayerChannelHandlerImpl(player as CraftPlayer)

    override fun createBundler(initialCapacity: Int): PacketBundler = bundlerOf(initialCapacity)
    override fun createLazyBundler(): PacketBundler = lazyBundlerOf()
    override fun createParallelBundler(threshold: Int): PacketBundler = parallelBundlerOf(threshold)

    override fun create(location: Location, yOffset: Double, initialConsumer: Consumer<ModelDisplay>): ModelDisplay = ModelDisplayImpl(ItemDisplay(EntityType.ITEM_DISPLAY, (location.world as CraftWorld).handle).apply {
        entityData[Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID] = 3
        billboardConstraints = Display.BillboardConstraints.FIXED
        valid = true
        moveTo(
            location.x,
            location.y,
            location.z,
            location.yaw,
            0F
        )
        itemTransform = ItemDisplayContext.FIXED
    }, yOffset).apply {
        initialConsumer.accept(this)
        display.entityData.packDirty()
    }

    override fun createNametag(bone: RenderedBone): ModelNametag = ModelNametagImpl(bone)

    override fun tint(itemStack: ItemStack, rgb: Int): ItemStack {
        return itemStack.asVanilla().apply {
            set(DataComponents.DYED_COLOR, DyedItemColor(rgb))
            set(DataComponents.CUSTOM_MODEL_DATA, get(DataComponents.CUSTOM_MODEL_DATA)?.let {
                CustomModelData(it.floats, it.flags, it.strings, it.colors
                    .run {
                        if (rgb == 0xFFFFFF) this else map { color ->
                            ARGB.multiply(color, rgb) and 0xFFFFFF
                        }
                    }
                    .ifEmpty { listOf(rgb) })
            })
        }.asBukkit()
    }

    override fun createHitBox(entity: BaseEntity, bone: RenderedBone, namedBoundingBox: NamedBoundingBox, mountController: MountController, listener: HitBoxListener): HitBox? {
        val handle = entity.handle() as? Entity ?: return null
        val newBox = namedBoundingBox.center()
        return HitBoxImpl(
            namedBoundingBox.name,
            newBox,
            bone,
            listener,
            handle,
            mountController
        ).craftEntity
    }
    override fun version(): NMSVersion = NMSVersion.V1_21_R5

    override fun adapt(entity: org.bukkit.entity.Entity): BaseBukkitEntity {
        entity as CraftEntity
        return BaseEntityImpl(entity)
    }

    override fun adapt(player: Player): BaseBukkitPlayer {
        player as CraftPlayer
        return BasePlayerImpl(
            player,
            dirtyChecked(
                { getGameProfile(player.handle) },
                { ModelGameProfile(it) },
                { a, b -> a == b && a.properties["texture"] === b.properties["texture"]}
            ),
            dirtyChecked({ player.handle.toCustomisation() }, { PlayerSkinParts(it) })
        )
    }

    override fun profile(player: Player): ModelProfile = ModelGameProfile(getGameProfile((player as CraftPlayer).handle))

    override fun createPlayerHead(profile: ModelProfile): ItemStack = VanillaItemStack(Items.PLAYER_HEAD).apply {
        set(DataComponents.PROFILE, ResolvableProfile(GameProfile(
            profile.info().id,
            profile.info().name ?: "",
        ).apply {
            properties.put("textures", Property("textures", profile.skin().raw))
        }))
    }.asBukkit()

    override fun createSkinItem(model: String, flags: List<Boolean>, strings: List<String>, colors: List<Int>): TransformedItemStack {
        return VanillaItemStack(Items.PLAYER_HEAD).run {
            set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(emptyList(), flags, strings, colors))
            set(DataComponents.ITEM_MODEL, ResourceLocation.parse(model))
            TransformedItemStack.of(asBukkit())
        }
    }

    override fun isProxyOnlineMode(): Boolean = ONLINE_MODE
}
