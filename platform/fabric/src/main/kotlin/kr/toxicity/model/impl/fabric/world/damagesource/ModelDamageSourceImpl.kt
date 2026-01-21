/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.impl.fabric.world.damagesource

import kr.toxicity.model.api.event.ModelDamageSource
import kr.toxicity.model.api.fabric.platform.FabricEntity
import kr.toxicity.model.api.fabric.platform.FabricLocation
import kr.toxicity.model.api.platform.PlatformEntity
import kr.toxicity.model.api.platform.PlatformLocation
import net.minecraft.world.damagesource.DamageSource

class ModelDamageSourceImpl(private val source: DamageSource) : ModelDamageSource {
    override fun getCausingEntity(): PlatformEntity? = source.entity?.let { FabricEntity(it) }

    override fun getDirectEntity(): PlatformEntity? = source.directEntity?.let { FabricEntity(it) }

    override fun getDamageLocation(): PlatformLocation? {
        return source.sourcePositionRaw()?.let { pos ->
            FabricLocation(
                source.entity?.level(),
                pos.x, pos.y, pos.z,
                0f, 0f
            )
        }
    }

    override fun getSourceLocation(): PlatformLocation? {
        return source.sourcePosition?.let { pos ->
            FabricLocation(
                source.entity?.level(),
                pos.x, pos.y, pos.z,
                0f, 0f
            )
        }
    }

    override fun isIndirect(): Boolean = !source.isDirect

    override fun getFoodExhaustion(): Float = source.foodExhaustion

    override fun scalesWithDifficulty(): Boolean = source.scalesWithDifficulty()
}
