package kr.toxicity.model.manager

import kr.toxicity.model.api.pack.PackZipper

interface GlobalManager {
    fun start() {}
    fun reload(pipeline: ReloadPipeline, zipper: PackZipper)
    fun end() {}
}