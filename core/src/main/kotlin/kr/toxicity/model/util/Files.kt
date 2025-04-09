package kr.toxicity.model.util

import java.io.File

fun File.subFolder(name: String) = File(this, name).apply {
    if (!exists()) mkdirs()
}

fun File.forEach(block: (File) -> Unit) {
    listFiles()?.sortedBy {
        it.name
    }?.forEach(block)
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
