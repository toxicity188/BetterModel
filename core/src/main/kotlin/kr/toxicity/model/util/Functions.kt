package kr.toxicity.model.util

import kr.toxicity.model.api.bone.BoneTagRegistry
import java.math.BigDecimal
import java.text.DecimalFormat

val BYTE_UNIT = BigDecimal("1024.000")
val COMMA_FORMAT = DecimalFormat("#,###")
val COMMA_DECIMAL_FORMAT = DecimalFormat("#,###.000")

inline fun <T> T?.ifNull(lazyMessage: () -> String): T & Any = this ?: throw RuntimeException(lazyMessage())

fun Number.withComma(): String = COMMA_FORMAT.format(this)
val String.boneName get() = BoneTagRegistry.parse(this)

fun Long.toByteFormat(): String {
    var value = BigDecimal("$this.000")
    for (format in LengthFormat.entries) {
        if (value < BYTE_UNIT) return "${COMMA_DECIMAL_FORMAT.format(value)} ${format.name}"
        value /= BYTE_UNIT
    }
    return "${COMMA_DECIMAL_FORMAT.format(value)} ${LengthFormat.entries.last().name}"
}

enum class LengthFormat {
    B,
    KB,
    MB,
    GB
}