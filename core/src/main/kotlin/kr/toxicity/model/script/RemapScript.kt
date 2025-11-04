/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.script

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.util.any
import kr.toxicity.model.util.toPackName
import kr.toxicity.model.util.toSet

class RemapScript(
    model: String,
    map: String?
) : AnimationScript {

    private val newModel by lazy {
        model.toPackName().let {
            BetterModel.modelOrNull(it)
        }
    }
    private val filter by lazy {
        map?.let {
            BetterModel.modelOrNull(it.toPackName())?.flatten()?.map { group ->
                group.name()
            }?.toSet()
        }
    }

    override fun accept(tracker: Tracker) {
        val f = filter
        newModel?.run {
            if (flatten().filter {
                f == null || f.contains(it.name())
            }.any {
                tracker.bone(it.name())?.itemStack({ true }, it.itemStack) == true
            }) tracker.forceUpdate(true)
        }
    }

    override fun isSync(): Boolean = false
}