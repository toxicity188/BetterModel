/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit.nms.v1_21_R3

import kr.toxicity.model.api.event.ModelDamageSource
import kr.toxicity.model.api.platform.PlatformEntity
import kr.toxicity.model.api.platform.PlatformLocation
import net.minecraft.world.damagesource.DamageSource
import org.bukkit.craftbukkit.util.CraftLocation

internal class ModelDamageSourceImpl(
    private val source: DamageSource
) : ModelDamageSource {
    override fun getCausingEntity(): PlatformEntity? = source.entity?.bukkitEntity?.wrap()
    override fun getDirectEntity(): PlatformEntity? = source.directEntity?.bukkitEntity?.wrap()
    override fun getDamageLocation(): PlatformLocation? = source.sourcePositionRaw()?.let {
        CraftLocation.toBukkit(it, causingEntity?.unwarp()?.world).wrap()
    }
    override fun getSourceLocation(): PlatformLocation? = source.sourcePosition?.let {
        CraftLocation.toBukkit(it, causingEntity?.unwarp()?.world).wrap()
    }
    override fun isIndirect(): Boolean = !source.isDirect
    override fun getFoodExhaustion(): Float = source.foodExhaustion
    override fun scalesWithDifficulty(): Boolean = source.scalesWithDifficulty()
}
