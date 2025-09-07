package kr.toxicity.model.script

import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.api.tracker.TrackerUpdateAction
import kr.toxicity.model.api.util.function.BonePredicate

class PartVisibilityScript(
    val predicate: BonePredicate,
    val visible: Boolean
) : AnimationScript {

    override fun accept(tracker: Tracker) {
        tracker.update(
            TrackerUpdateAction.togglePart(visible),
            predicate
        )
    }

    override fun isSync(): Boolean = false
}