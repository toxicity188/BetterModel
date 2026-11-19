/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.placeholder

import io.lumine.mythic.core.skills.placeholders.PlaceholderContext
import io.lumine.mythic.core.skills.placeholders.types.GenericPlaceholder
import io.lumine.mythic.core.skills.placeholders.types.GenericPlaceholderTypes.StringPlaceholder
import io.lumine.mythic.core.utils.annotations.MythicPlaceholder
import kr.toxicity.model.bukkit.compatibility.mythicmobs.toRegistry

@MythicPlaceholder(placeholder = "caster.model.yaw")
internal class CasterModelYawPlaceholder(args: GenericPlaceholderArguments) :
    GenericPlaceholder<String>(args), StringPlaceholder {
    override fun applyWithMetaKeywords(context: PlaceholderContext): String =
        context.meta()?.caster?.entity?.toRegistry()?.first()?.rotation()?.y()?.toString() ?: "[No Model]"
}
