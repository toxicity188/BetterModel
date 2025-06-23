package kr.toxicity.model.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.raw.ModelData
import kr.toxicity.model.api.nms.NMSVersion
import java.io.File

fun File.toModel(): ModelBlueprint = bufferedReader().use {
    ModelBlueprint.from(nameWithoutExtension.toPackName(), ModelData.GSON.fromJson(it, ModelData::class.java))
}

fun JsonElement.toByteArray(): ByteArray {
    return ModelData.GSON.toJson(this).toByteArray(Charsets.UTF_8)
}

val PACK_MCMETA
    get() = JsonObject().apply {
        add("pack", JsonObject().apply {
            addProperty("pack_format", PLUGIN.nms().version().metaVersion)
            addProperty("description", "BetterModel's default pack")
            add("supported_formats", JsonArray(2).apply {
                add(NMSVersion.first().metaVersion)
                add(NMSVersion.latest().metaVersion)
            })
        })
    }
