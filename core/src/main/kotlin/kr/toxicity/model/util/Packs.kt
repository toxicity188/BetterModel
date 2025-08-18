package kr.toxicity.model.util

import com.google.gson.*
import kr.toxicity.model.api.BetterModelConfig
import kr.toxicity.model.api.BetterModelConfig.PackType.*
import kr.toxicity.model.api.pack.PackData
import kr.toxicity.model.api.pack.PackPath
import kr.toxicity.model.api.pack.PackResource
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.manager.ReloadPipeline
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun JsonElement.estimatedSize(): Long {
    when (this) {
        is JsonObject -> return entrySet().sumOf { it.value.estimatedSize() }
        is JsonArray -> return asList().sumOf { it.estimatedSize() }
        is JsonNull -> return 8L
        is JsonPrimitive -> {
            if (isNumber) return asInt / 5L + 6L
            if (isBoolean) return if (asBoolean) 8L else 10L
            if (isString) return 2L * asString.length
        }
        else -> {}
    }
    return 0L
}

fun BetterModelConfig.PackType.toGenerator() = when (this) {
    FOLDER -> FolderGenerator()
    ZIP -> ZipGenerator()
    NONE -> NoneGenerator()
}

interface PackGenerator {
    fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackData
}

class FolderGenerator : PackGenerator {
    private val time = System.currentTimeMillis()
    private val file = File(DATA_FOLDER.parent, CONFIG.buildFolderLocation())
    private val fileTree = sortedMapOf<String, File>(Comparator.reverseOrder()).apply {
        val l = file.path.length + 1
        file.forEach { sub ->
            sub.forEachAll {
                put(it.path.substring(l), it)
            }
        }
    }

    private fun PackPath.toFile(): File {
        val replaced = path.replace('/', File.separatorChar)
        return synchronized(fileTree) {
            fileTree.remove(replaced)
        } ?: File(file, replaced).apply {
            parentFile.mkdirs()
        }

    }

    override fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackData {
        val map = ConcurrentHashMap<PackPath, ByteArray>()
        pipeline.forEachParallel(zipper.build(), PackResource::estimatedSize) {
            val bytes = it.get()
            map[it.path()] = bytes
            it.path().toFile().outputStream().buffered().use { output ->
                output.write(bytes)
            }
            pipeline.progress()
            debugPack {
                "This file was successfully zipped: ${it.path()}"
            }
        }
        fileTree.values.forEach(File::delete)
        return PackData(map.toImmutableView(), System.currentTimeMillis() - time)
    }
}

class ZipGenerator : PackGenerator {
    private val time = System.currentTimeMillis()
    private val file = File(DATA_FOLDER.parent, "${CONFIG.buildFolderLocation()}.zip")

    override fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackData {
        val map = ConcurrentHashMap<PackPath, ByteArray>()
        ZipOutputStream(runCatching {
            MessageDigest.getInstance("SHA-1")
        }.map {
            DigestOutputStream(file.outputStream().buffered(), it)
        }.getOrElse {
            file.outputStream().buffered()
        }).use { zip ->
            zip.setLevel(Deflater.BEST_COMPRESSION)
            zip.setComment("BetterModel's generated resource pack.")
            pipeline.forEachParallel(zipper.build(), PackResource::estimatedSize) {
                val result = it.get()
                map[it.path()] = result
                synchronized(zip) {
                    zip.putNextEntry(ZipEntry(it.path().path()))
                    zip.write(result)
                    zip.closeEntry()
                }
                pipeline.progress()
                debugPack {
                    "This file was successfully zipped: ${it.path()}"
                }
            }
        }
        return PackData(map.toImmutableView(), System.currentTimeMillis() - time)
    }
}

class NoneGenerator : PackGenerator {
    private val time = System.currentTimeMillis()

    override fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackData {
        val map = ConcurrentHashMap<PackPath, ByteArray>()
        pipeline.forEachParallel(zipper.build(), PackResource::estimatedSize) {
            map[it.path()] = it.get()
            pipeline.progress()
        }
        return PackData(map.toImmutableView(), System.currentTimeMillis() - time)
    }
}