/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric

import com.google.common.collect.ImmutableMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import kr.toxicity.model.api.bone.RenderedBone
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import kr.toxicity.model.api.entity.BaseEntity
import kr.toxicity.model.api.entity.BasePlayer
import kr.toxicity.model.api.fabric.BetterModelFabric
import kr.toxicity.model.api.fabric.platform.FabricItemStack
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.*
import kr.toxicity.model.api.platform.PlatformEntity
import kr.toxicity.model.api.platform.PlatformItemStack
import kr.toxicity.model.api.platform.PlatformLocation
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.player.PlayerSkinParts
import kr.toxicity.model.api.profile.ModelProfile
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.fabric.entity.*
import kr.toxicity.model.fabric.mixin.DisplayAccessor
import kr.toxicity.model.fabric.mixin.EntityAccessor
import kr.toxicity.model.fabric.network.*
import kr.toxicity.model.fabric.profile.ModelProfileImpl
import kr.toxicity.model.util.PLATFORM
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.util.ARGB
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.component.ResolvableProfile
import java.util.function.Consumer

class BetterModelNMSImpl : NMS {
    override fun create(
        location: PlatformLocation,
        yOffset: Double,
        initialConsumer: Consumer<ModelDisplay>
    ): ModelDisplay {
        val type = EntityType.ITEM_DISPLAY
        val level = location.asFabric.level()!!

        val itemDisplay = Display.ItemDisplay(type, level).apply {
            billboardConstraints = Display.BillboardConstraints.FIXED
            entityData[DisplayAccessor.getDataPosRotInterpolationDurationId()] = 3
            itemTransform = ItemDisplayContext.FIXED
            snapTo(location.x(), location.y(), location.z(), location.yaw(), 0.0f)
        }

        val modelDisplay = ModelDisplayEntityImpl(itemDisplay, yOffset).apply {
            initialConsumer.accept(this)
            display.entityData.packDirty()
        }

        return modelDisplay
    }

    override fun createNametag(bone: RenderedBone): ModelNametag = ModelNametagImpl(bone)

    override fun inject(player: PlatformPlayer): PlayerChannelHandler = PlayerChannelHandlerImpl(player.asFabric.player)

    override fun createBundler(initialCapacity: Int): PacketBundler = bundlerOf(initialCapacity)

    override fun createLazyBundler(): PacketBundler = lazyBundlerOf()

    override fun createParallelBundler(threshold: Int): PacketBundler = parallelBundlerOf(threshold)

    override fun tint(itemStack: PlatformItemStack, rgb: Int): PlatformItemStack {
        val stack = itemStack.asFabric.stack.apply {
            set(
                DataComponents.DYED_COLOR,
                DyedItemColor(rgb)
            )
            set(
                DataComponents.CUSTOM_MODEL_DATA,
                get(DataComponents.CUSTOM_MODEL_DATA)?.withMappedColors(rgb)
            )
        }

        return FabricItemStack(stack)
    }

    private fun CustomModelData.withMappedColors(rgb: Int): CustomModelData {
        return CustomModelData(
            floats,
            flags,
            strings,
            getMappedColors(rgb)
        )
    }

    private fun CustomModelData.getMappedColors(rgb: Int): List<Int> {
        if (colors.isEmpty()) {
            return listOf(rgb)
        }

        if (rgb == 0xFFFFFF) {
            return colors
        }

        return colors.map { color ->
            ARGB.multiply(color, rgb) and 0xFFFFFF
        }
    }

    override fun mount(registry: EntityTrackerRegistry, bundler: PacketBundler) {
        (registry.entity().handle() as? Entity)?.let {
            bundler += registry.mountPacket(it)
        }
    }

    override fun hide(channel: PlayerChannelHandler, registry: EntityTrackerRegistry) {
        val target = registry.entity().handle() as? Entity ?: return
        val bundlers = bundlerOf()

        val dataValues = target.entityData.pack(
            valueFilter = { value ->
                value.id == EntityAccessor.getDataSharedFlagsId().id
            }
        )
        dataValues?.let {
            val packet = ClientboundSetEntityDataPacket(target.id, it)
            bundlers.add(
                packet.toRegistryDataPacket(channel.uuid(), registry)
            )
        }

        if (target is LivingEntity) {
            val packet = if (registry.hideOption(channel.uuid()).equipment) {
                target.toEquipmentPacket { ItemStack.EMPTY }
            } else {
                target.toEquipmentPacket()
            }

            packet?.let { bundlers += it }
        }

        bundlers.send(channel.player())
    }

    override fun createHitBox(
        entity: BaseEntity,
        bone: RenderedBone,
        boundingBox: ModelBoundingBox,
        controller: MountController,
        listener: HitBoxListener
    ): HitBox {
        return HitBoxEntityImpl(
            boundingBox.center(),
            bone,
            listener,
            entity.handle() as Entity,
            controller
        )
    }

    override fun version(): NMSVersion = NMSVersion.V1_21_R7

    override fun adapt(entity: PlatformEntity): BaseEntity = BaseFabricEntityImpl(entity.asFabric.entity)

    override fun adapt(player: PlatformPlayer): BasePlayer {
        val player = player.asFabric.player
        return BaseFabricPlayerImpl(
            player,
            dirtyChecked(
                { player.gameProfile },
                { ModelProfileImpl(it) }
            ),
            dirtyChecked(
                { player.getCustomisation() },
                { PlayerSkinParts(it) }
            )
        )
    }

    override fun profile(player: PlatformPlayer): ModelProfile = ModelProfileImpl(player.asFabric.player.gameProfile)

    override fun createPlayerHead(profile: ModelProfile): PlatformItemStack = Items.PLAYER_HEAD.defaultInstance
        .apply {
            val gameProfileProperty = ImmutableMultimap.of(
                "textures",
                Property("textures", profile.skin().raw)
            )
            val gameProfile = GameProfile(
                profile.info().id,
                profile.info().name ?: "",
                PropertyMap(gameProfileProperty)
            )
            set(DataComponents.PROFILE, ResolvableProfile.createResolved(gameProfile))
        }
        .let {
            FabricItemStack(it)
        }

    override fun isProxyOnlineMode(): Boolean = (PLATFORM as BetterModelFabric).server().usesAuthentication()
}
