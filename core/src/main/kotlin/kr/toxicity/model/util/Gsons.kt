/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.raw.ModelData
import java.io.File

fun File.toTexturedModel(): ModelBlueprint? = runCatching {
    reader().use {
        ModelData.GSON.fromJson(it, ModelData::class.java)
            .apply { assertSupported() }
            .loadBlueprint(nameWithoutExtension.toPackName())
            .let { result ->
                if (result.errors.isNotEmpty()) warn(
                    *buildList {
                        add("Error has been occurred while parsing this model: ${result.blueprint.name}")
                        addAll(result.errors)
                    }.map { error -> error.toComponent() }.toTypedArray()
                )
                result.blueprint
            }
    }
}.handleFailure {
    "Unable to load this model: $path"
}.getOrNull()

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