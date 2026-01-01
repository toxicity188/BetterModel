/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.util

import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import javax.imageio.ImageIO

inline fun File.getOrCreateDirectory(name: String, initialConsumer: (File) -> Unit = {}) = File(this, name).also { target ->
    if (!target.exists()) {
        target.mkdirs()
        initialConsumer(target)
    }
}

fun File.subFiles(): List<File> = listFiles()?.toList() ?: emptyList()

inline fun copyResourceAs(name: String, block: (InputStream) -> Unit) {
    PLUGIN.getResource(name)?.use(block)
}

fun File.toImage(): BufferedImage = ImageIO.read(this)

fun File.fileTrees(): Stream<Path> = Files.find(
    toPath(),
    Int.MAX_VALUE,
    { _, attr ->
        !attr.isDirectory
    }
)

fun File.addResource(name: String) {
    copyResourceAs(name) { input ->
        File(this, name).outputStream().use {
            it.buffered().use { output -> input.copyTo(output) }
        }
    }
}
