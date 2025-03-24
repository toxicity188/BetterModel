package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.mythicmobs.MM_PART_ID
import kr.toxicity.model.manager.ModelManagerImpl

class ChangePartMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val part = mlc.getString(MM_PART_ID)!!
    private val nmid = mlc.getString(arrayOf("newmodelid", "nm", "nmid", "newmodel"))
    private val newPart = mlc.getString(arrayOf("newpart", "np", "npid"))

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val model = ModelManagerImpl.renderer(nmid)?.groupByTree(newPart)?.itemStack ?: return SkillResult.ERROR
        return EntityTracker.tracker(p0.caster.entity.bukkitEntity.uniqueId)?.let {
            it.itemStack({
                it.name == part
            }, model)
            it.forceUpdate(true)
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}