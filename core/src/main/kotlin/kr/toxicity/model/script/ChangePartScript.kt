package kr.toxicity.model.script

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bone.BoneName
import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.api.tracker.TrackerUpdateAction
import kr.toxicity.model.api.util.function.BonePredicate

class ChangePartScript(
    val predicate: BonePredicate,
    newModel: String,
    newPart: BoneName
) : AnimationScript {

    private val model by lazy {
        BetterModel.modelOrNull(newModel)?.groupByTree(newPart)?.itemStack
    }

    override fun accept(tracker: Tracker) {
        model?.let {
            tracker.update(
                TrackerUpdateAction.itemStack(it),
                predicate
            )
        }
    }

    override fun isSync(): Boolean = false
}