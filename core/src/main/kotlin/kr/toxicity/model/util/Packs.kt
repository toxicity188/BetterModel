package kr.toxicity.model.util

import com.google.gson.*
import kr.toxicity.model.api.BetterModelConfig
import kr.toxicity.model.api.BetterModelConfig.PackType.*
import kr.toxicity.model.api.pack.*
import kr.toxicity.model.manager.ReloadPipeline
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.pathString

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
    val exists: Boolean
    fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackResult
}

class FolderGenerator : PackGenerator {
    private val file = File(DATA_FOLDER.parent, CONFIG.buildFolderLocation())
    override val exists: Boolean = file.exists()
    private val fileTree by lazy {
        sortedMapOf<String, Path>(Comparator.reverseOrder()).apply {
            val after = CONFIG.buildFolderLocation() + File.separatorChar
            Files.walk(file.apply {
                mkdirs()
            }.toPath()).use { stream ->
                stream.forEach {
                    put(it.pathString.substringAfter(after), it)
                }
            }
        }
    }

    private fun PackPath.toFile(): File {
        val replaced = path.replace('/', File.separatorChar)
        return synchronized(fileTree) {
            fileTree.remove(replaced)?.toFile()
        } ?: File(file, replaced).apply {
            parentFile.mkdirs()
        }
    }

    override fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackResult {
        val build = zipper.build()
        val pack = PackResult(build.meta(), file)
        pipeline.forEachParallel(build.resources(), PackResource::estimatedSize) {
            val bytes = it.get()
            pack[it.overlay()] = PackByte(it.path(), bytes)
            val file = it.path().toFile()
            if (file.length() != bytes.size.toLong()) {
                file.writeBytes(bytes)
                debugPack {
                    "This file was successfully generated: ${it.path()}"
                }
            }
            pipeline.progress()
        }
        fileTree.values.forEach {
            it.toFile().delete()
        }
        return pack.apply {
            freeze()
        }
    }
}

class ZipGenerator : PackGenerator {
    private val file = File(DATA_FOLDER.parent, "${CONFIG.buildFolderLocation()}.zip")
    override val exists: Boolean = file.exists()

    override fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackResult {
        val build = zipper.build()
        val pack = PackResult(build.meta(), file)
        ZipOutputStream(runCatching {
            MessageDigest.getInstance("SHA-1")
        }.map {
            DigestOutputStream(file.outputStream().buffered(), it)
        }.getOrElse {
            file.outputStream().buffered()
        }).use { zip ->
            zip.setLevel(Deflater.BEST_COMPRESSION)
            zip.setComment("BetterModel's generated resource pack.")
            pipeline.forEachParallel(build.resources(), PackResource::estimatedSize) {
                val bytes = it.get()
                pack[it.overlay()] = PackByte(it.path(), bytes)
                synchronized(zip) {
                    zip.putNextEntry(ZipEntry(it.path().path()))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                pipeline.progress()
                debugPack {
                    "This file was successfully zipped: ${it.path()}"
                }
            }
        }
        return pack.apply {
            freeze()
        }
    }
}

class NoneGenerator : PackGenerator {
    override val exists: Boolean = false
    override fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackResult {
        val build = zipper.build()
        val pack = PackResult(build.meta(), null)
        pipeline.forEachParallel(build.resources(), PackResource::estimatedSize) {
            pack[it.overlay()] = PackByte(it.path(), it.get())
            pipeline.progress()
        }
        return pack.apply {
            freeze()
        }
    }
}