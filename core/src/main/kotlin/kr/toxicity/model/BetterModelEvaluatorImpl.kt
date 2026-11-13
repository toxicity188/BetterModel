/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model

import kr.toxicity.model.api.BetterModelEvaluator
import kr.toxicity.model.api.animation.keyframe.Keyframe
import kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext
import kr.toxicity.model.api.util.function.FloatSupplier
import org.redlance.mocha.parser.MolangParser
import org.redlance.mocha.runtime.IsConstantExpression
import org.redlance.mocha.runtime.MolangInterpreter
import org.redlance.mocha.runtime.value.MutableObjectBinding
import org.redlance.mocha.runtime.value.NumberValue
import org.redlance.mocha.runtime.value.ObjectValue

class BetterModelEvaluatorImpl : BetterModelEvaluator {

    private val molang = MolangInterpreter.standard()

    override fun newContext(): KeyframeRuntimeContext = KeyframeRuntimeContextImpl()

    private class KeyframeRuntimeContextImpl : KeyframeRuntimeContext {
        val bind = MutableObjectBinding()

        override fun set(key: String, value: Float) {
            bind.set(key, NumberValue.of(value))
        }

        override fun setFunction(key: String, supplier: FloatSupplier) {
            bind.setFunction(key, ObjectValue.FloatFunction { supplier.asFloat })
        }
    }

    override fun compile(expression: String): Keyframe {
        val compiled = MolangParser.parseAll(expression)
        return if (compiled.all { IsConstantExpression.test(it, molang.scope()) }) Keyframe.of(molang.eval(compiled))
        else Keyframe {
            (it as? KeyframeRuntimeContextImpl)?.bind?.let { bind ->
                molang.eval(compiled) { scope ->
                    scope.set("query", bind)
                    scope.set("q", bind)
                }
            } ?: molang.eval(compiled)
        }
    }
}
