package kr.toxicity.model.util

import kr.toxicity.model.api.event.ModelEvent
import kr.toxicity.model.api.util.EventUtil

inline fun <reified T : ModelEvent> callEvent(noinline block: () -> T): Boolean = EventUtil.call(T::class.java) { block() }.triggered()
