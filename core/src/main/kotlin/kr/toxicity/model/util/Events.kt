/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.util

import kr.toxicity.model.api.event.ModelEvent
import kr.toxicity.model.api.util.EventUtil

inline fun <reified T : ModelEvent> callEvent(noinline block: () -> T): Boolean = EventUtil.call(T::class.java) { block() }.triggered()
