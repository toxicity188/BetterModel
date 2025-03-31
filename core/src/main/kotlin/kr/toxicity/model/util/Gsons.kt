package kr.toxicity.model.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.raw.ModelData
import kr.toxicity.model.api.nms.NMSVersion
import kr.toxicity.model.api.player.PlayerLimb
import kr.toxicity.model.manager.ConfigManagerImpl
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

fun String.toLimb() = runCatching {
    PlayerLimb.valueOf(uppercase())
}.getOrNull()

fun JsonElement.toByteArray(): ByteArray {
    val sb = StringBuilder()
    ModelData.GSON.toJson(this, sb)
    return sb.toString().toByteArray(Charsets.UTF_8)
}

fun versionRangeOf(min: Int, max: Int) = JsonObject().apply {
    addProperty("min_inclusive", min)
    addProperty("max_inclusive", max)
}

val PACK_MCMETA
    get() = JsonObject().apply {
        add("pack", JsonObject().apply {
            addProperty("pack_format", PLUGIN.nms().version().metaVersion)
            addProperty("description", "BetterModel's default pack")
            add("supported_formats", versionRangeOf(
                NMSVersion.first().metaVersion,
                NMSVersion.latest().metaVersion
            ))
        })
        add("overlays", JsonObject().apply {
            add("entries", JsonArray().apply {
                add(JsonObject().apply {
                    add("formats", versionRangeOf(NMSVersion.first().metaVersion, 45))
                    addProperty("directory", "${ConfigManagerImpl.namespace()}_legacy")
                })
                add(JsonObject().apply {
                    add("formats", versionRangeOf(46, NMSVersion.latest().metaVersion))
                    addProperty("directory", "${ConfigManagerImpl.namespace()}_modern")
                })
            })
        })
    }
