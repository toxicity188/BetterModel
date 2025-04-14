package kr.toxicity.model.compatibility.mythicmobs

import io.lumine.mythic.api.config.MythicLineConfig
import kr.toxicity.model.api.bone.RenderedBone
import kr.toxicity.model.api.util.BonePredicate

val MM_PART_ID = arrayOf("partid", "p", "pid", "part")
val MM_CHILDREN = arrayOf("children", "child")
val MM_EXACT_MATCH = arrayOf("exactmatch", "em", "exact", "match")

val MythicLineConfig.bonePredicateNullable: BonePredicate
    get() = getString(MM_PART_ID).let { part ->
        if (part == null) BonePredicate.TRUE else {
            val predicate: (RenderedBone) -> Boolean = if (getBoolean(MM_EXACT_MATCH, true)) {
                { b ->
                    b.name == part
                }
            } else {
                { b ->
                    b.name.contains(part, ignoreCase = true)
                }
            }
            BonePredicate.of(getBoolean(MM_CHILDREN, false), predicate)
        }
    }
val MythicLineConfig.bonePredicate
    get() = getString(MM_PART_ID)!!.let { part ->
        val predicate: (RenderedBone) -> Boolean = if (getBoolean(MM_EXACT_MATCH, true)) {
            { b ->
                b.name == part
            }
        } else {
            { b ->
                b.name.contains(part, ignoreCase = true)
            }
        }
        BonePredicate.of(getBoolean(MM_CHILDREN, false), predicate)
    }