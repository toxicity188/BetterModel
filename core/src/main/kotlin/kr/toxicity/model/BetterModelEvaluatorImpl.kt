/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model

import kr.toxicity.model.api.BetterModelEvaluator
import kr.toxicity.model.api.util.function.Float2FloatFunction
import team.unnamed.mocha.MochaEngine
import team.unnamed.mocha.runtime.value.NumberValue
import team.unnamed.mocha.runtime.value.ObjectProperty
import team.unnamed.mocha.runtime.value.ObjectValue

class BetterModelEvaluatorImpl : BetterModelEvaluator {

    private val mocha = MochaEngine.createStandard()

    override fun compile(expression: String): Float2FloatFunction {
        val compiled = mocha.prepareEval(expression)
        return Float2FloatFunction {
            synchronized(mocha) {
                QueryBinding(it).run {
                    mocha.scope()["query"] = this
                    mocha.scope()["q"] = this
                }
                compiled.evaluate().toFloat()
            }
        }
    }

    private class QueryBinding(
        animTime: Float
    ) : ObjectValue {

        private companion object {
            val zero = ObjectProperty.property(NumberValue.zero(), true)
        }

        private val animTime = ObjectProperty.property(NumberValue.of(animTime.toDouble()), true)

        override fun getProperty(name: String): ObjectProperty {
            return when (name) {
                "anim_time", "life_time" -> animTime
                else -> zero
            }
        }
    }
}
