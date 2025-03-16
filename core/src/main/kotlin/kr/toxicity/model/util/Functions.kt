package kr.toxicity.model.util

fun <T> T?.ifNull(message: String): T & Any = this ?: throw RuntimeException(message)

fun rgb(r: Int, g: Int, b: Int) = ((r and 255) shl 16) or ((g and 255) shl 8) or (b and 255)