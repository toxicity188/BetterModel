package kr.toxicity.model.compatibility.mythicmobs

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.SkillMetadata
import kr.toxicity.model.api.util.function.BonePredicate
import kr.toxicity.model.util.boneName
import kr.toxicity.model.util.toPackName
import kr.toxicity.model.util.toRegistry
import kr.toxicity.model.util.toTracker

val MM_MODEL_ID = arrayOf("mid", "m", "model")
val MM_PART_ID = arrayOf("partid", "p", "pid", "part")
val MM_CHILDREN = arrayOf("children", "child")
val MM_EXACT_MATCH = arrayOf("exactmatch", "em", "exact", "match")
val MM_SEAT = arrayOf("seat", "p", "pbone")

fun SkillMetadata.toRegistry() = caster.entity.toRegistry()
fun SkillMetadata.toTracker(model: String?) = caster.entity.toTracker(model)
fun AbstractEntity.toTracker(model: String?) = bukkitEntity.toTracker(model)
fun AbstractEntity.toRegistry() = bukkitEntity.toRegistry()

fun MythicLineConfig.toPlaceholderString(array: Array<String>, defaultValue: String? = null) = toPlaceholderString(array, defaultValue) { it }
fun <T> MythicLineConfig.toPlaceholderStringList(array: Array<String>, mapper: (List<String>) -> T) = toPlaceholderString(array) {
    mapper(it?.split(",") ?: emptyList())
}
fun <T> MythicLineConfig.toPlaceholderString(array: Array<String>, defaultValue: String? = null, mapper: (String?) -> T): (PlaceholderArgument) -> T {
    return getPlaceholderString(array, defaultValue)?.let {
        { meta ->
            mapper(when (meta) {
                is PlaceholderArgument.None -> it.get()
                is PlaceholderArgument.SkillMeta -> it[meta.meta]
                is PlaceholderArgument.TargetedSkillMeta -> it.get(meta.meta, meta.target)
                is PlaceholderArgument.Entity -> it[meta.entity]
            })
        }
    } ?: mapper(null).let { mapped ->
        {
            mapped
        }
    }
}
fun MythicLineConfig.toPlaceholderInteger(array: Array<String>, defaultValue: Int = 0) = toPlaceholderInteger(array, defaultValue) { it ?: defaultValue }
fun MythicLineConfig.toNullablePlaceholderInteger(array: Array<String>) = toPlaceholderInteger(array, null) { it }
fun <T> MythicLineConfig.toPlaceholderInteger(array: Array<String>, defaultValue: Int? = null, mapper: (Int?) -> T): (PlaceholderArgument) -> T {
    return getPlaceholderInteger(array, defaultValue?.toString())?.let {
        { meta ->
            mapper(when (meta) {
                is PlaceholderArgument.None -> it.get()
                is PlaceholderArgument.SkillMeta -> it[meta.meta]
                is PlaceholderArgument.TargetedSkillMeta -> it.get(meta.meta, meta.target)
                is PlaceholderArgument.Entity -> it[meta.entity]
            })
        }
    } ?: mapper(null).let { mapped ->
        {
            mapped
        }
    }
}
fun MythicLineConfig.toPlaceholderFloat(array: Array<String>, defaultValue: Float = 0F) = toPlaceholderFloat(array, defaultValue) { it ?: defaultValue }
fun MythicLineConfig.toNullablePlaceholderFloat(array: Array<String>) = toPlaceholderFloat(array, null) { it }
fun <T> MythicLineConfig.toPlaceholderFloat(array: Array<String>, defaultValue: Float? = null, mapper: (Float?) -> T): (PlaceholderArgument) -> T {
    return getPlaceholderFloat(array, defaultValue?.toString())?.let {
        { meta ->
            mapper(when (meta) {
                is PlaceholderArgument.None -> it.get()
                is PlaceholderArgument.SkillMeta -> it[meta.meta]
                is PlaceholderArgument.TargetedSkillMeta -> it.get(meta.meta, meta.target)
                is PlaceholderArgument.Entity -> it[meta.entity]
            })
        }
    } ?: mapper(null).let { mapped ->
        {
            mapped
        }
    }
}
fun MythicLineConfig.toPlaceholderBoolean(array: Array<String>, defaultValue: Boolean? = null) = toPlaceholderBoolean(array, defaultValue) { it == true }
fun MythicLineConfig.toNullablePlaceholderBoolean(array: Array<String>, defaultValue: Boolean? = null) = toPlaceholderBoolean(array, defaultValue) { it }
fun <T> MythicLineConfig.toPlaceholderBoolean(array: Array<String>, defaultValue: Boolean? = null, mapper: (Boolean?) -> T): (PlaceholderArgument) -> T {
    return getPlaceholderBoolean(array, defaultValue)?.let {
        { meta ->
            mapper(when (meta) {
                is PlaceholderArgument.None -> it.get()
                is PlaceholderArgument.SkillMeta -> it[meta.meta]
                is PlaceholderArgument.TargetedSkillMeta -> it.get(meta.meta, meta.target)
                is PlaceholderArgument.Entity -> it[meta.entity]
            })
        }
    } ?: mapper(null).let { mapped ->
        {
            mapped
        }
    }
}
fun MythicLineConfig.toPlaceholderColor(array: Array<String>, defaultValue: String = "FFFFFF") = toPlaceholderColor(array, defaultValue) { it }
fun <T> MythicLineConfig.toPlaceholderColor(array: Array<String>, defaultValue: String = "FFFFFF", mapper: (Int?) -> T): (PlaceholderArgument) -> T {
    return toPlaceholderString(array, defaultValue) {
        mapper(it?.toIntOrNull(16))
    }
}

val MythicLineConfig.bonePredicateNullable
    get() = toBonePredicate(BonePredicate.TRUE)
val MythicLineConfig.bonePredicate
    get() = toBonePredicate(BonePredicate.FALSE)

val MythicLineConfig.modelPlaceholder
    get() = toPlaceholderString(MM_MODEL_ID) {
        it?.toPackName()
    }

fun MythicLineConfig.toBonePredicate(defaultPredicate: BonePredicate): (PlaceholderArgument) -> BonePredicate {
    val match = toPlaceholderBoolean(MM_EXACT_MATCH, true)
    val children = toPlaceholderBoolean(MM_CHILDREN, false)
    val partSupplier = toPlaceholderString(MM_PART_ID) {
        it?.boneName?.name
    }
    return { meta ->
        val part = partSupplier(meta)
        if (part == null) defaultPredicate else {
            BonePredicate.of(if (children(meta)) BonePredicate.State.TRUE else BonePredicate.State.FALSE, if (match(meta)) {
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
}

fun SkillMetadata.toPlaceholderArgs() = PlaceholderArgument.SkillMeta(this)
fun AbstractEntity.toPlaceholderArgs() = PlaceholderArgument.Entity(this)
fun toPlaceholderArgs(meta: SkillMetadata, target: AbstractEntity) = PlaceholderArgument.TargetedSkillMeta(meta, target)

sealed interface PlaceholderArgument {
    data object None : PlaceholderArgument
    data class SkillMeta(val meta: SkillMetadata) : PlaceholderArgument
    data class TargetedSkillMeta(val meta: SkillMetadata, val target: AbstractEntity) : PlaceholderArgument
    data class Entity(val entity: AbstractEntity) : PlaceholderArgument
}