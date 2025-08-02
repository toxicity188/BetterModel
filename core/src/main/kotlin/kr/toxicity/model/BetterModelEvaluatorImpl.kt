package kr.toxicity.model

import gg.moonflower.molangcompiler.api.MolangCompiler
import gg.moonflower.molangcompiler.api.MolangRuntime
import kr.toxicity.model.api.BetterModelEvaluator

class BetterModelEvaluatorImpl : BetterModelEvaluator {

    private val molang = MolangCompiler.create(MolangCompiler.DEFAULT_FLAGS, javaClass.classLoader)

    private fun Float.query() = MolangRuntime.runtime()
        .setQuery("life_time", this)
        .setQuery("anim_time", this)
        .create()

    override fun evaluate(expression: String, time: Float): Float {
        return time.query().safeResolve(molang.compile(expression))
    }
}