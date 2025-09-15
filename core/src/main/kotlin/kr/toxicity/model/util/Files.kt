package kr.toxicity.model.util

import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

inline fun File.getOrCreateDirectory(name: String, initialConsumer: (File) -> Unit = {}) = File(this, name).also { target ->
    if (!target.exists()) {
        target.mkdirs()
        initialConsumer(target)
    }
}

inline fun copyResourceAs(name: String, outputCreator: () -> OutputStream) {
    PLUGIN.getResource(name)?.use { input ->
        outputCreator().use {
            it.buffered().use { buffered ->
                input.copyTo(buffered)
            }
        }
    }
}

fun File.fileTreeList(): Stream<Path> = Files.find(
    toPath(),
    Int.MAX_VALUE,
    { _, attr ->
        !attr.isDirectory
    }
)

fun File.addResource(name: String) {
    copyResourceAs(name) {
        File(this, name).outputStream()
    }
}