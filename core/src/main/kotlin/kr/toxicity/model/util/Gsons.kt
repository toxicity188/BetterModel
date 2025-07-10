package kr.toxicity.model.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.raw.ModelData
import java.io.File

fun File.toModel(): ModelBlueprint = bufferedReader().use {
    ModelBlueprint.from(nameWithoutExtension.toPackName(), ModelData.GSON.fromJson(it, ModelData::class.java))
}

fun JsonElement.toByteArray(): ByteArray {
    return ModelData.GSON.toJson(this).toByteArray(Charsets.UTF_8)
}

fun buildJsonArray(capacity: Int = 10, block: JsonArray.() -> Unit) = JsonArray(capacity).apply(block)
fun buildJsonObject(block: JsonObject.() -> Unit) = JsonObject().apply(block)

fun jsonArrayOf(vararg element: Any?) = buildJsonArray {
    element.filterNotNull().forEach {
        add(it.toJsonElement())
    }
}

fun jsonObjectOf(vararg element: Pair<String, Any>) = buildJsonObject {
    element.forEach {
        add(it.first, it.second.toJsonElement())
    }
}

operator fun JsonArray.plusAssign(other: JsonElement) {
    add(other)
}

fun Any.toJsonElement(): JsonElement = when (this) {
    is String -> JsonPrimitive(this)
    is Char -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is JsonElement -> this
    is List<*> -> run {
        val map = mapNotNull {
            it?.toJsonElement()
        }
        buildJsonArray(map.size) {
            map.forEach {
                add(it)
            }
        }
    }
    is Map<*, *> -> buildJsonObject {
        forEach {
            add(it.key?.toString() ?: return@forEach, it.value?.toJsonElement() ?: return@forEach)
        }
    }
    else -> throw RuntimeException("Unsupported type: ${javaClass.name}")
}