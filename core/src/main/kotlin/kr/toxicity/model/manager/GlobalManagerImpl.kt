package kr.toxicity.model.manager

import kr.toxicity.model.api.manager.GlobalManager

interface GlobalManagerImpl : GlobalManager {
    fun start() {}
    fun end() {}
}