package kr.toxicity.model.util

import com.google.gson.JsonElement
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.raw.ModelData
import java.io.File
import java.io.InputStream

fun File.toModel(): ModelBlueprint = bufferedReader().use {
    ModelBlueprint.from(nameWithoutExtension, ModelData.GSON.fromJson(it, ModelData::class.java))
}
fun InputStream.toModel(name: String = "unknown"): ModelBlueprint = bufferedReader().use {
    ModelBlueprint.from(name, ModelData.GSON.fromJson(it, ModelData::class.java))
}

fun JsonElement.save(file: File) {
    file.bufferedWriter().use {
        ModelData.GSON.toJson(this, it)
    }
}