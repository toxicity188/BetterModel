package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.compatibility.mythicmobs.modelPlaceholder
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.compatibility.mythicmobs.toTracker
import kr.toxicity.model.manager.ModelManagerImpl
import kr.toxicity.model.util.any
import kr.toxicity.model.util.toPackName
import kr.toxicity.model.util.toSet

class RemapModelMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val modelId = mlc.modelPlaceholder
    private val newModelId = mlc.toPlaceholderString(arrayOf("newmodelid", "n", "nid", "newmodel")) {
        it?.toPackName()?.let { name ->
            ModelManagerImpl.renderer(name)
        }
    }
    private val map = mlc.toPlaceholderString(arrayOf("map")) {
        it?.toPackName()?.let { name ->
            ModelManagerImpl.renderer(name)?.flatten()?.map { group ->
                group.name
            }?.toSet()
        }
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        val m = modelId(args) ?: return SkillResult.INVALID_CONFIG
        val nm = newModelId(args) ?: return SkillResult.INVALID_CONFIG
        val filter = map(args)
        p0.toTracker(m)?.let { tracker ->
            if (nm.flatten().filter {
                filter == null || filter.contains(it.name)
            }.any {
                tracker.bone(it.name)?.itemStack({ true }, it.itemStack) == true
            }) tracker.forceUpdate(true)
        }
        return SkillResult.SUCCESS
    }
}