/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric

import com.google.common.collect.ImmutableMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import kr.toxicity.model.api.bone.RenderedBone
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import kr.toxicity.model.api.entity.BaseEntity
import kr.toxicity.model.api.entity.BasePlayer
import kr.toxicity.model.api.fabric.BetterModelFabric
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.*
import kr.toxicity.model.api.platform.PlatformEntity
import kr.toxicity.model.api.platform.PlatformItemStack
import kr.toxicity.model.api.platform.PlatformLocation
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.player.PlayerSkinParts
import kr.toxicity.model.api.profile.ModelProfile
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.util.TransformedItemStack
import kr.toxicity.model.impl.fabric.entity.*
import kr.toxicity.model.impl.fabric.network.*
import kr.toxicity.model.impl.fabric.profile.ModelProfileImpl
import kr.toxicity.model.mixin.DisplayAccessor
import kr.toxicity.model.mixin.EntityAccessor
import kr.toxicity.model.util.PLATFORM
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.resources.Identifier
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
import org.joml.Vector3d
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
            entityData[DisplayAccessor.`bettermodel$getDataPosRotInterpolationDurationId`()] = 3
            itemTransform = ItemDisplayContext.FIXED
            yRot = location.yaw()
        }

        val modelDisplay = ModelDisplayEntityImpl(Vector3d(location.x(), location.y(), location.z()), itemDisplay, yOffset).apply {
            initialConsumer.accept(this)
            display.entityData.packDirty()
        }

        return modelDisplay
    }

    override fun createNametag(bone: RenderedBone): ModelNametag = ModelNametagImpl(bone)

    override fun inject(player: PlatformPlayer): PlayerChannelHandler = PlayerChannelHandlerImpl(player.unwarp())

    override fun createBundler(initialCapacity: Int): PacketBundler = bundlerOf(initialCapacity)

    override fun createParallelBundler(threshold: Int): PacketBundler = parallelBundlerOf(threshold)

    override fun tint(itemStack: PlatformItemStack, rgb: Int): PlatformItemStack {
        return itemStack.clone().unwarp().apply {
            set(DataComponents.DYED_COLOR, DyedItemColor(rgb))
            set(DataComponents.CUSTOM_MODEL_DATA, get(DataComponents.CUSTOM_MODEL_DATA)?.withMappedColors(rgb))
        }.wrap()
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
        val list = bundlerOf()
        target.entityData.pack(
            valueFilter = { it.id == EntityAccessor.`bettermodel$getDataSharedFlagsId`().id }
        )?.let {
            list += ClientboundSetEntityDataPacket(target.id, it).toRegistryDataPacket(channel.uuid(), registry)
        }
        if (target is LivingEntity) {
            val packet = if (registry.hideOption(channel.uuid()).equipment) target.toEmptyEquipmentPacket() else target.toEquipmentPacket()
            packet?.let { list += it }
        }
        list.send(channel.player())
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

    override fun version(): NMSVersion = NMSVersion.V26_R1

    override fun adapt(entity: PlatformEntity): BaseEntity = BaseFabricEntityImpl(entity.unwarp())

    override fun adapt(player: PlatformPlayer): BasePlayer {
        val connection = player.unwarp()
        return BaseFabricPlayerImpl(
            connection, dirtyChecked(
                { connection.player.gameProfile },
                { ModelProfileImpl(it) }
            ),
            dirtyChecked(
                { connection.player.getCustomisation() },
                { PlayerSkinParts(it) }
            )
        )
    }

    override fun profile(player: PlatformPlayer): ModelProfile = ModelProfileImpl(player.unwarp().player.gameProfile)

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
        }.wrap()

    override fun isProxyOnlineMode(): Boolean = (PLATFORM as BetterModelFabric).server().usesAuthentication()

    override fun createSkinItem(model: String, floats: List<Float>, flags: List<Boolean>, strings: List<String>, colors: List<Int>): TransformedItemStack {
        return ItemStack(Items.PLAYER_HEAD).run {
            set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(floats, flags, strings, colors))
            set(DataComponents.ITEM_MODEL, Identifier.parse(model))
            TransformedItemStack.of(wrap())
        }
    }
}
