package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic

abstract class AbstractSkillMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "bm", mlc)