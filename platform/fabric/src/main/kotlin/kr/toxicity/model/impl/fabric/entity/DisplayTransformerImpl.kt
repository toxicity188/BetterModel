/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.impl.fabric.entity

import kr.toxicity.model.api.nms.AnimationBundler
import kr.toxicity.model.api.nms.DisplayTransformer
import kr.toxicity.model.api.nms.PacketBundler
import kr.toxicity.model.api.util.lock.SingleLock
import kr.toxicity.model.impl.fabric.network.plusAssign
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Display
import org.joml.Quaternionf
import org.joml.Vector3f

class DisplayTransformerImpl(source: Display.ItemDisplay) :
    DisplayTransformer
{
    private val id = source.id

    private val entityData = TransformationData()
    private val entityDataLock = SingleLock()

    override fun transform(
        duration: Int,
        position: Vector3f,
        scale: Vector3f,
        rotation: Quaternionf,
        bundler: AnimationBundler
    ) {
        entityDataLock.accessToLock {
            entityData.transform(
                duration,
                position,
                scale,
                rotation
            )
            entityData.packDirty(id, bundler)
        }
    }

    override fun sendTransformation(bundler: PacketBundler) {
        entityDataLock.accessToLock {
            entityData.pack()
        }?.let {
            bundler += ClientboundSetEntityDataPacket(id, it)
        }
    }
}
