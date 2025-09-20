/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_21_R6

import com.mojang.math.Transformation
import kr.toxicity.model.api.bone.RenderedBone
import kr.toxicity.model.api.nms.ModelNametag
import kr.toxicity.model.api.nms.PacketBundler
import kr.toxicity.model.api.util.EntityUtil
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.entity.Player
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class ModelNametagImpl(
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
        MinecraftServer.getServer().overworld()
    ).apply {
        entityData[Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID] = 3
        setTransformation(emptyTransformation)
        billboardConstraints = Display.BillboardConstraints.CENTER
    }
    private var alwaysVisible = false
    private var location = Location(
        null,
        0.0,
        0.0,
        0.0
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun component(component: Component?) {
        display.text = component?.asVanilla()
    }

    override fun teleport(location: Location) {
        this.location = location
    }

    override fun alwaysVisible(alwaysVisible: Boolean) {
        this.alwaysVisible = alwaysVisible
    }

    override fun send(player: Player) {
        if (display.text == null) return
        val hb = bone.group.hitBox?.centerPoint() ?: emptyVector
        val pos = bone.worldPosition(hb, emptyVector, player.uniqueId)
        display.moveTo(Vec3(
            location.x + pos.x,
            location.y + pos.y,
            location.z + pos.z
        ))
        val inPoint = alwaysVisible || EntityUtil.isCustomNameVisible(player.location, location)
        when {
            inPoint && viewedPlayer.add(player.uniqueId) -> bundlerOfNotNull(
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
            viewedPlayer.remove(player.uniqueId) -> bundlerOf(removePacket)
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