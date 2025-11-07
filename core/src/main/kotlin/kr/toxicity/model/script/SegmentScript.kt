/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.script

import kr.toxicity.model.api.bone.SegmentBone
import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.api.tracker.TrackerUpdateAction
import kr.toxicity.model.api.util.function.BonePredicate

class SegmentScript(
    val predicate: BonePredicate,
    val bounded: Boolean?,
    val minX: Float?,
    val maxX: Float?,
    val minY: Float?,
    val maxY: Float?,
    val minZ: Float?,
    val maxZ: Float?,
    val alignRate: Float?,
    val elasticity: Float?
) : AnimationScript {

    override fun accept(tracker: Tracker) {
        tracker.pipeline.iterateTree { bone ->
            if (predicate.test(bone) && bone is SegmentBone) {
                bounded?.let { bone.setBounded(it) }
                minX?.let { bone.setMinX(it) }
                maxX?.let { bone.setMaxX(it) }
                minY?.let { bone.setMinY(it) }
                maxY?.let { bone.setMaxY(it) }
                minZ?.let { bone.setMinZ(it) }
                maxZ?.let { bone.setMaxZ(it) }
                alignRate?.let { bone.setAlignRate(it) }
                elasticity?.let { bone.setElasticity(it) }
            }
        }
    }

    override fun isSync(): Boolean = false
}
