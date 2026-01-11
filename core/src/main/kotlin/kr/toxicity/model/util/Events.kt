package kr.toxicity.model.util

import kr.toxicity.model.api.event.ModelEvent
import kr.toxicity.model.api.util.EventUtil

fun ModelEvent.call(): Boolean = EventUtil.call(this)
