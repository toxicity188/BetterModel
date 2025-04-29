package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.compatibility.mythicmobs.bonePredicate
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.compatibility.mythicmobs.toTracker
import kr.toxicity.model.manager.ModelManagerImpl
import kr.toxicity.model.util.boneName

class ChangePartMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

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
        return p0.toTracker()?.let {
            if (it.itemStack(predicate(args), model)) it.forceUpdate(true)
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}