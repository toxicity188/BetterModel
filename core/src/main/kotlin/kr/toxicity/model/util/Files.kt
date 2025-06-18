package kr.toxicity.model.util

import java.io.File
import java.io.OutputStream

fun File.getOrCreateDirectory(name: String, initialConsumer: (File) -> Unit) = File(this, name).also { target ->
    if (!target.exists()) {
        target.mkdirs()
        initialConsumer(target)
    }
}

fun File.forEach(block: (File) -> Unit) {
    listFiles()?.forEach(block)
}

fun File.fileTreeList() = mutableListOf<File>().apply {
    forEachAllFolder(::add)
}

fun File.forEachAllFolder(block: (File) -> Unit) {
    if (isDirectory) forEach {
        it.forEachAllFolder(block)
    } else {
        block(this)
    }
}

fun File.forEachAll(block: (File) -> Unit) {
    if (isDirectory) forEach {
        block(this)
        it.forEachAll(block)
    } else {
        block(this)
    }
}

fun File.addResourceAs(name: String) {
    copyResourceAs(name) {
        File(this, name).outputStream().buffered()
    }
}

fun copyResourceAs(name: String, outputCreator: () -> OutputStream) {
    PLUGIN.getResource(name)?.buffered()?.use { input ->
        outputCreator().use {
            input.copyTo(it)
        }
    }
}