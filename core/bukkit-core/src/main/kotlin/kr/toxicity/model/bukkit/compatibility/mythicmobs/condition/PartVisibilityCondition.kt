/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.condition

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import kr.toxicity.model.bukkit.compatibility.mythicmobs.megModelId
import kr.toxicity.model.bukkit.compatibility.mythicmobs.selectedTrackers
import kr.toxicity.model.bukkit.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.bukkit.compatibility.mythicmobs.toPlaceholderString

internal class PartVisibilityCondition(config: MythicLineConfig) : IEntityCondition {
    private val model = config.megModelId
    private val part = config.toPlaceholderString(arrayOf("p", "pid", "part", "partid"), "")
    private val visible = config.getBoolean(arrayOf("v", "visible", "visibility"), true)
    private val exact = config.getBoolean(arrayOf("em", "exact", "match", "exactmatch"), true)
    private val child = config.getBoolean(arrayOf("c", "ch", "child"), false)

    override fun check(target: AbstractEntity): Boolean {
        val args = target.toPlaceholderArgs()
        val id = part(args).orEmpty().lowercase()
        var matched = false
        for (tracker in target.selectedTrackers(model(args))) for (bone in tracker.bones()) {
            val display = bone.display ?: continue
            if (generateSequence(bone) { if (child) it.parent else null }.none {
                val name = it.name().name
                id.isEmpty() || (if (exact) name == id else name.contains(id))
            }) continue
            matched = true
            if (display.invisible() == visible) return false
        }
        return matched
    }
}
