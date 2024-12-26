package kr.toxicity.model.util

fun <T> T?.ifNull(message: String): T & Any = this ?: throw RuntimeException(message)