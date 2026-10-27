/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.util

import kr.toxicity.model.api.BetterModelConfig
import kr.toxicity.model.api.BetterModelConfig.PackType.*
import kr.toxicity.model.api.pack.*
import kr.toxicity.model.manager.ReloadPipeline
import net.kyori.adventure.text.format.NamedTextColor
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.pathString
import kotlin.io.use

fun BetterModelConfig.PackType.toGenerator() = when (this) {
    FOLDER -> FolderGenerator()
    ZIP -> ZipGenerator()
    NONE -> NoneGenerator()
}

interface PackGenerator {
    val exists: Boolean
    fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackResult
    fun hashChanged(result: PackResult): Boolean {
        val hash = result.hash().toString()
        return File(DATA_FOLDER.getOrCreateDirectory(".cache"), "zip-hash.txt").run {
            if (!exists || !exists() || readText() != hash) {
                writeText(hash)
                true
            } else false
        }
    }
}

class FolderGenerator : PackGenerator {
    private val file = File(DATA_FOLDER.parent, CONFIG.buildFolderLocation())
    override val exists: Boolean = file.exists()

    private val fileTree by lazy {
        (CONFIG.buildFolderLocation() + File.separatorChar).let { after ->
            Files.walk(file.apply {
                mkdirs()
            }.toPath()).use { stream ->
                stream.collect(Collectors.toConcurrentMap(
                    { path -> path.pathString.substringAfter(after) },
                    { it }
                ))
            }
        }
    }

    private fun PackPath.toFile(): File {
        val replaced = path.replace('/', File.separatorChar)
        return fileTree.remove(replaced)?.toFile() ?: File(file, replaced).apply {
            parentFile.mkdirs()
        }
    }

    override fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackResult {
        val build = zipper.build()
        val pack = PackResult(build.meta(), file)
        val changed = AtomicBoolean()

        pipeline.forEachParallel(build.resources(), PackResource::estimatedSize) { pack[it.overlay()] = PackByte(it.path(), it.get()) }
        if (!hashChanged(pack) && file.exists()) return pack.apply { freeze() }

        pipeline.forEachParallel(pack.stream().toList(), PackByte::length) {
            val file = it.path().toFile()
            val index = pipeline.progress(it.path().name())
            if (file.exists() && file.length() == it.length() && file.readBytes().contentEquals(it.bytes)) return@forEachParallel
            file.writeBytes(it.bytes)
            changed.set(true)
            debugPack {
                componentOf(
                    "This file was successfully generated: ".toComponent(),
                    it.path().path.toComponent(NamedTextColor.GREEN),
                    " ($index/${pipeline.goal})".toComponent(NamedTextColor.DARK_GRAY)
                )
            }
        }
        fileTree.values
            .sortedByDescending { it }
            .forEach {
                if (it.toFile().delete()) changed.set(true)
            }
        return pack.apply { freeze(changed.get()) }
    }
}

class ZipGenerator : PackGenerator {
    private val file = File(DATA_FOLDER.parent, "${CONFIG.buildFolderLocation()}.zip")
    override val exists: Boolean = file.exists()

    override fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackResult {
        return zipper.writeToResult(pipeline, file).apply {
            freeze(hashChanged(this))
        }.apply {
            if (!changed()) return this
            fun zip(zip: ZipOutputStream) {
                zip.setLevel(Deflater.BEST_COMPRESSION)
                zip.setComment("BetterModel's generated resource pack.")
                stream().forEach {
                    zip.putNextEntry(ZipEntry(it.path().path()))
                    zip.write(it.bytes())
                    zip.closeEntry()
                }
            }
            file.outputStream().use {
                it.buffered().use { buffered ->
                    ZipOutputStream(buffered).use(::zip)
                }
            }
        }
    }
}

class NoneGenerator : PackGenerator {
    override val exists: Boolean = false
    override fun create(zipper: PackZipper, pipeline: ReloadPipeline): PackResult {
        return zipper.writeToResult(pipeline).apply {
            freeze()
        }
    }
}

fun PackZipper.writeToResult(pipeline: ReloadPipeline, dir: File? = null): PackResult {
    val build = build()
    return PackResult(build.meta(), dir).apply {
        pipeline.forEachParallel(build.resources(), PackResource::estimatedSize) {
            set(it.overlay(), PackByte(it.path(), it.get()))
            val index = pipeline.progress(it.path().name())
            debugPack {
                componentOf(
                    "This file was successfully zipped: ".toComponent(),
                    it.path().path.toComponent(NamedTextColor.GREEN),
                    " ($index/${pipeline.goal})".toComponent(NamedTextColor.DARK_GRAY)
                )
            }
        }
    }
}
