package kr.toxicity.model.manager.debug

import kr.toxicity.model.manager.ReloadPipeline

interface ReloadIndicator {
    infix fun status(status: ReloadPipeline.Status)
    fun close()
}