/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.world.damagesource

import kr.toxicity.model.api.event.ModelDamageSource
import kr.toxicity.model.api.mod.platform.ModLocation
import kr.toxicity.model.api.platform.PlatformEntity
import kr.toxicity.model.api.platform.PlatformLocation
import kr.toxicity.model.impl.fabric.wrap
import net.minecraft.world.damagesource.DamageSource

class ModelDamageSourceImpl(private val source: DamageSource) : ModelDamageSource {
    override fun getCausingEntity(): PlatformEntity? = source.entity?.wrap()

    override fun getDirectEntity(): PlatformEntity? = source.directEntity?.wrap()

    override fun getDamageLocation(): PlatformLocation? {
        return source.sourcePositionRaw()?.let { pos ->
            ModLocation.of(
                source.entity?.level(),
                pos.x, pos.y, pos.z,
                0f, 0f
            )
        }
    }

    override fun getSourceLocation(): PlatformLocation? {
        return source.sourcePosition?.let { pos ->
            ModLocation.of(
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
