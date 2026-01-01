/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.util

import kr.toxicity.model.api.config.IndicatorConfig
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.manager.debug.BossBarIndicator
import kr.toxicity.model.manager.debug.ReloadIndicator
import java.util.*

private typealias Type = IndicatorConfig.IndicatorOption

private val INDICATOR_MAP = EnumMap<Type, (ReloadInfo) -> ReloadIndicator?>(Type::class.java).apply {
    put(Type.PROGRESS_BAR) {
        BossBarIndicator(it.sender.audience())
    }
}

fun Type.toIndicator(info: ReloadInfo) = INDICATOR_MAP[this]?.invoke(info)
fun Iterable<Type>.toIndicator(info: ReloadInfo) = mapNotNull {
    it.toIndicator(info)
}