/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model

import kr.toxicity.model.api.BetterModelEvaluator
import kr.toxicity.model.api.util.function.Float2FloatFunction
import org.redlance.mocha.parser.MolangParser
import org.redlance.mocha.runtime.MolangInterpreter
import org.redlance.mocha.runtime.value.MutableObjectBinding
import org.redlance.mocha.runtime.value.NumberValue

class BetterModelEvaluatorImpl : BetterModelEvaluator {

    private val molang = MolangInterpreter.standard()

    override fun compile(expression: String): Float2FloatFunction {
        val compiled = MolangParser.parseAll(expression)
        val bind = MutableObjectBinding()
        return Float2FloatFunction {
            NumberValue.of(it).let { value ->
                bind.set("anim_time", value)
                bind.set("life_time", value)
            }
            molang.eval(compiled) { scope ->
                scope.set("query", bind)
                scope.set("q", bind)
            }
        }
    }
}
