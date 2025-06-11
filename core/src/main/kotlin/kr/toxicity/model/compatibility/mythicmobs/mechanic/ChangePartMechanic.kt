package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.compatibility.mythicmobs.*
import kr.toxicity.model.manager.ModelManagerImpl
import kr.toxicity.model.util.boneName

class ChangePartMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val predicate = mlc.bonePredicate
    private val nmid = mlc.toPlaceholderString(arrayOf("newmodelid", "nm", "nmid", "newmodel"))
    private val newPart = mlc.toPlaceholderString(arrayOf("newpart", "np", "npid")) {
        it?.boneName
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        val n1 = nmid(args) ?: return SkillResult.CONDITION_FAILED
        val n2 = newPart(args) ?: return SkillResult.CONDITION_FAILED
        val model = ModelManagerImpl.renderer(n1)?.groupByTree(n2)?.itemStack ?: return SkillResult.CONDITION_FAILED
        return p0.toTracker(model(args))?.let {
            if (it.itemStack(predicate(args), model)) it.forceUpdate(true)
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}