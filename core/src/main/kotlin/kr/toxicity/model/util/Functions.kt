package kr.toxicity.model.util

import java.text.DecimalFormat

val COMMA_FORMAT = DecimalFormat("#,###")

fun <T> T?.ifNull(lazyMessage: () -> String): T & Any = this ?: throw RuntimeException(lazyMessage())
fun Number.withComma(): String = COMMA_FORMAT.format(this)
