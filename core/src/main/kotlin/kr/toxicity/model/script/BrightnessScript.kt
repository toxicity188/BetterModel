/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.script

import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.api.tracker.TrackerUpdateAction
import kr.toxicity.model.api.util.function.BonePredicate

class BrightnessScript(
    val predicate: BonePredicate,
    val block: Int,
    val sky: Int
) : AnimationScript {

    override fun accept(tracker: Tracker) {
        tracker.update(
            TrackerUpdateAction.brightness(block, sky),
            predicate
        )
    }

    override fun isSync(): Boolean = false
}
