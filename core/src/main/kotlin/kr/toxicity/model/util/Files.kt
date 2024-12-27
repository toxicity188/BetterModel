package kr.toxicity.model.util

import java.io.File

fun File.subFolder(name: String) = File(this, name).apply {
    if (!exists()) mkdirs()
}
fun File.clear() = apply {
    deleteRecursively()
    mkdirs()
}
fun File.subFile(name: String) = File(this, name)

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
