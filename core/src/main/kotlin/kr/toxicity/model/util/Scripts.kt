package kr.toxicity.model.util

import kr.toxicity.model.api.script.ScriptBuilder
import kr.toxicity.model.api.util.function.BonePredicate

val ScriptBuilder.ScriptMetaData.bonePredicate get(): BonePredicate {
    val match = asBoolean("exact") != false
    val children = asBoolean("children") == true
    val part = asString("part")?.boneName?.name
    return if (part == null) BonePredicate.TRUE else {
        BonePredicate.of(if (children) BonePredicate.State.TRUE else BonePredicate.State.FALSE, if (match) {
            { b ->
                b.name().name == part
            }
        } else {
            { b ->
                b.name().name.contains(part, ignoreCase = true)
            }
        })
    }
}