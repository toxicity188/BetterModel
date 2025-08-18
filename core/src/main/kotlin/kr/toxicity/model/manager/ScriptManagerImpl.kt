package kr.toxicity.model.manager

import kr.toxicity.model.api.event.AnimationSignalEvent
import kr.toxicity.model.api.manager.ScriptManager
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.script.EntityScriptBuilder
import kr.toxicity.model.util.call

object ScriptManagerImpl : ScriptManager, GlobalManager {

    private val scriptMap = hashMapOf<String, EntityScriptBuilder>()

    init {
        addBuilder("signal") {
            AnimationScript.of script@ { source ->
                source.pipeline.allPlayer().forEach { player ->
                    AnimationSignalEvent(player, it).call()
                }
            }
        }
    }

    override fun build(script: String): AnimationScript? = scriptMap[script.substringBefore(':')]?.build(script.substringAfter(':'))

    override fun addBuilder(name: String, script: EntityScriptBuilder) {
        scriptMap[name] = script
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
    }
}