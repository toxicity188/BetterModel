/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.impl.fabric.entity

import com.mojang.math.Transformation
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bone.BoneMovement
import kr.toxicity.model.api.bone.BonePosition
import kr.toxicity.model.api.bone.RenderedBone
import kr.toxicity.model.api.fabric.BetterModelFabric
import kr.toxicity.model.api.nms.ModelNametag
import kr.toxicity.model.api.nms.PacketBundler
import kr.toxicity.model.api.platform.PlatformLocation
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.util.EntityUtil
import kr.toxicity.model.impl.fabric.chat.asVanilla
import kr.toxicity.model.impl.fabric.network.bundlerOf
import kr.toxicity.model.impl.fabric.network.bundlerOfNotNull
import kr.toxicity.model.impl.fabric.network.pack
import kr.toxicity.model.impl.fabric.network.plusAssign
import kr.toxicity.model.mixin.DisplayAccessor
import kr.toxicity.model.util.PLATFORM
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ModelNametagImpl(
    private val bone: RenderedBone
) : ModelNametag {
    private companion object {
        private val emptyVector = Vector3f()
        private val emptyTransformation = Transformation(
            Vector3f(-1F / 40F, -0.2F - 1F / 40F, 0F),
            null,
            null,
            null
        )
    }

    private val viewedPlayer = ConcurrentHashMap.newKeySet<UUID>()
    private val display = Display.TextDisplay(
        EntityType.TEXT_DISPLAY,
        (PLATFORM as BetterModelFabric).server().overworld()
    ).apply {
        entityData[DisplayAccessor.`bettermodel$getDataPosRotInterpolationDurationId`()] = 3
        setTransformation(emptyTransformation)
        billboardConstraints = Display.BillboardConstraints.CENTER
    }
    private val posCache = BoneMovement()
    private var alwaysVisible = false
    private var location = BetterModel.platform().adapter().zero()

    override fun component(component: net.kyori.adventure.text.Component?) {
        display.text = component?.asVanilla() ?: Component.empty()
    }

    override fun teleport(location: PlatformLocation) {
        this.location = location
    }

    override fun alwaysVisible(alwaysVisible: Boolean) {
        this.alwaysVisible = alwaysVisible
    }

    override fun send(player: PlatformPlayer) {
        if (display.text == Component.empty()) return
        val hb = bone.group.hitBox?.centerPoint() ?: emptyVector
        val pos = bone.worldPosition(BonePosition(emptyVector, hb, player.uuid()), posCache)
        display.snapTo(Vec3(
            location.x() + pos.x,
            location.y() + pos.y,
            location.z() + pos.z
        ))
        val inPoint = alwaysVisible || EntityUtil.isCustomNameVisible(player.location(), location)
        when {
            inPoint && viewedPlayer.add(player.uuid()) -> bundlerOfNotNull(
                addPacket,
                display.entityData.pack()?.let {
                    ClientboundSetEntityDataPacket(display.id, it)
                }
            )
            inPoint -> bundlerOfNotNull(
                ClientboundEntityPositionSyncPacket(display.id, PositionMoveRotation.of(display), false),
                display.entityData.packDirty()?.let {
                    ClientboundSetEntityDataPacket(display.id, it)
                }
            )
            viewedPlayer.remove(player.uuid()) -> bundlerOf(removePacket)
            else -> null
        }?.send(player)
    }

    override fun remove(bundler: PacketBundler) {
        bundler += removePacket
    }

    private val addPacket get() = ClientboundAddEntityPacket(
        display.id,
        display.uuid,
        display.x,
        display.y,
        display.z,
        display.xRot,
        display.yRot,
        display.type,
        0,
        display.deltaMovement,
        display.yHeadRot.toDouble()
    )

    private val removePacket get() = ClientboundRemoveEntitiesPacket(display.id)
}
