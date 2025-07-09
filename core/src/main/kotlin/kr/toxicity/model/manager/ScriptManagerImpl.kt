package kr.toxicity.model.manager

import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.manager.ScriptManager
import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.script.EntityScriptBuilder

object ScriptManagerImpl : ScriptManager, GlobalManagerImpl {

    private val scriptMap = hashMapOf<String, EntityScriptBuilder>()

    override fun build(script: String): AnimationScript? = scriptMap[script.substringBefore(':')]?.build(script.substringAfter(':'))

    override fun addBuilder(name: String, script: EntityScriptBuilder) {
        scriptMap[name] = script
    }

    override fun reload(info: ReloadInfo) {
    }
}