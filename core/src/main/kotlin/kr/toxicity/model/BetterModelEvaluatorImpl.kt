/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model

import gg.moonflower.molangcompiler.api.MolangCompiler
import gg.moonflower.molangcompiler.api.MolangRuntime
import kr.toxicity.model.api.BetterModelEvaluator
import kr.toxicity.model.api.util.function.Float2FloatFunction

class BetterModelEvaluatorImpl : BetterModelEvaluator {

    private val molang = MolangCompiler.create(MolangCompiler.DEFAULT_FLAGS, javaClass.classLoader)

    private fun Float.query() = MolangRuntime.runtime()
        .setQuery("life_time", this)
        .setQuery("anim_time", this)
        .create()

    override fun compile(expression: String): Float2FloatFunction {
        val compiled = molang.compile(expression)
        return Float2FloatFunction {
            it.query().safeResolve(compiled)
        }
    }
}