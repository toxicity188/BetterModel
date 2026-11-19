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

internal class HasModelCondition(config: MythicLineConfig) : IEntityCondition {
    private val model = config.megModelId
    private val state = config.toPlaceholderString(arrayOf("s", "state"))

    override fun check(target: AbstractEntity): Boolean {
        val args = target.toPlaceholderArgs()
        val animation = state(args)?.lowercase()
        return target.selectedTrackers(model(args)).any { tracker ->
            animation == null || tracker.bones().any { it.runningAnimation()?.name() == animation }
        }
    }
}
