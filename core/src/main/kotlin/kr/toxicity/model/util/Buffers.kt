/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.util

import com.google.gson.JsonElement
import kr.toxicity.model.api.data.blueprint.BlueprintImage
import kr.toxicity.model.api.data.raw.ModelData
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

private val IO_BUFFER = ThreadLocal.withInitial { ByteArrayOutputStream(1024) }

fun BlueprintImage.toByteArray(): ByteArray {
    return image
}

fun JsonElement.toByteArray(): ByteArray {
    return IO_BUFFER.get().let { buffer ->
        buffer.reset()
        OutputStreamWriter(buffer, StandardCharsets.UTF_8).use {
            ModelData.GSON.toJson(this, it)
        }
        buffer.toByteArray()
    }
}