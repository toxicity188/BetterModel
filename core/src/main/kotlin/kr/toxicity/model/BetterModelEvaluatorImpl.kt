package kr.toxicity.model

import gg.moonflower.molangcompiler.api.MolangCompiler
import gg.moonflower.molangcompiler.api.MolangRuntime
import kr.toxicity.model.api.BetterModelEvaluator
import kr.toxicity.model.util.PLUGIN

class BetterModelEvaluatorImpl : BetterModelEvaluator {

    private companion object {
        private val molang by lazy {
            MolangCompiler.create(MolangCompiler.DEFAULT_FLAGS, PLUGIN.javaClass.classLoader)
        }
    }

    private fun Float.query() = MolangRuntime.runtime()
        .setQuery("life_time", this)
        .setQuery("anim_time", this)
        .create()

    override fun evaluate(expression: String, time: Float): Float {
        return time.query().safeResolve(molang.compile(expression))
    }
}