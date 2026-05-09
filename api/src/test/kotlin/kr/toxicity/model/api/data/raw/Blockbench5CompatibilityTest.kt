/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.raw

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelConfig
import kr.toxicity.model.api.BetterModelEvaluator
import kr.toxicity.model.api.BetterModelPlatform
import kr.toxicity.model.api.util.function.Float2FloatFunction
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class Blockbench5CompatibilityTest {
    @OptIn(ExperimentalTime::class)
    @Test
    fun loadsBlockbench5GroupedAnimation() {
        registerTestPlatform()

        val result = measureTimedValue {
            val data = ModelData.GSON.fromJson(blockbench5Model, ModelData::class.java)
            data.assertSupported()
            data.loadBlueprint("blockbench5_grouped_animation", false)
        }

        assertTrue(result.duration < 2.seconds, "Blockbench 5 grouped animation loading should not run away.")
        assertTrue(result.value.errors().isEmpty(), result.value.errors().joinToString("\n"))
        assertEquals(1, result.value.blueprint().animations().size)
        assertEquals(3, result.value.blueprint().animations().getValue("spin").animator().size)
    }

    private companion object {
        private const val ROOT = "00000000-0000-0000-0000-000000000001"
        private const val CHILD = "00000000-0000-0000-0000-000000000002"
        private const val LEAF = "00000000-0000-0000-0000-000000000003"
        private const val CUBE = "00000000-0000-0000-0000-000000000004"
        private const val ANIMATION = "00000000-0000-0000-0000-000000000005"

        private fun registerTestPlatform() {
            runCatching { BetterModel.platform() }.onSuccess { return }

            val config = Proxy.newProxyInstance(
                BetterModelConfig::class.java.classLoader,
                arrayOf(BetterModelConfig::class.java),
            ) { _, method, _ ->
                when (method.name) {
                    "lerpFrameTime" -> 0
                    else -> defaultReturn(method)
                }
            } as BetterModelConfig
            val evaluator = BetterModelEvaluator { Float2FloatFunction.ZERO }
            val platform = Proxy.newProxyInstance(
                BetterModelPlatform::class.java.classLoader,
                arrayOf(BetterModelPlatform::class.java),
            ) { _, method, _ ->
                when (method.name) {
                    "config" -> config
                    "evaluator" -> evaluator
                    "dataFolder" -> File(".")
                    else -> defaultReturn(method)
                }
            } as BetterModelPlatform

            BetterModel.register(platform)
        }

        private fun defaultReturn(method: Method): Any? = when (val type = method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0F
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> 0.toChar()
            java.lang.Void.TYPE -> null
            String::class.java -> ""
            else -> if (type.isEnum) type.enumConstants.firstOrNull() else null
        }

        private val blockbench5Model = """
            {
              "meta": {
                "format_version": "5.0",
                "model_format": "free",
                "box_uv": false
              },
              "name": "blockbench5_grouped_animation",
              "resolution": {
                "width": 16,
                "height": 16
              },
              "textures": [],
              "elements": [
                {
                  "name": "cube",
                  "uuid": "$CUBE",
                  "from": [0, 0, 0],
                  "to": [1, 1, 1],
                  "origin": [0, 0, 0],
                  "faces": {},
                  "type": "cube"
                }
              ],
              "groups": [
                {
                  "name": "root",
                  "uuid": "$ROOT",
                  "origin": [0, 0, 0],
                  "rotation": [0, 0, 0],
                  "visibility": true
                },
                {
                  "name": "child",
                  "uuid": "$CHILD",
                  "origin": [0, 0, 0],
                  "rotation": [0, 0, 0],
                  "visibility": true
                },
                {
                  "name": "leaf",
                  "uuid": "$LEAF",
                  "origin": [0, 0, 0],
                  "rotation": [0, 0, 0],
                  "visibility": true
                }
              ],
              "outliner": [
                {
                  "name": "root",
                  "uuid": "$ROOT",
                  "origin": [0, 0, 0],
                  "children": [
                    {
                      "name": "child",
                      "uuid": "$CHILD",
                      "origin": [0, 0, 0],
                      "children": [
                        {
                          "name": "leaf",
                          "uuid": "$LEAF",
                          "origin": [0, 0, 0],
                          "children": [
                            "$CUBE"
                          ]
                        }
                      ]
                    }
                  ]
                }
              ],
              "animations": [
                {
                  "name": "spin",
                  "loop": "once",
                  "override": false,
                  "uuid": "$ANIMATION",
                  "length": 1,
                  "animators": {
                    "$ROOT": {
                      "name": "root",
                      "type": "bone",
                      "rotation_global": false,
                      "keyframes": [
                        {
                          "channel": "rotation",
                          "time": 0,
                          "interpolation": "linear",
                          "data_points": [
                            {
                              "x": 0,
                              "y": 0,
                              "z": 0
                            }
                          ]
                        },
                        {
                          "channel": "rotation",
                          "time": 1,
                          "interpolation": "linear",
                          "data_points": [
                            {
                              "x": 720,
                              "y": 0,
                              "z": 0
                            }
                          ]
                        }
                      ]
                    },
                    "$CHILD": {
                      "name": "child",
                      "type": "bone",
                      "rotation_global": false,
                      "keyframes": [
                        {
                          "channel": "rotation",
                          "time": 0,
                          "interpolation": "linear",
                          "data_points": [
                            {
                              "x": 0,
                              "y": 0,
                              "z": 0
                            }
                          ]
                        },
                        {
                          "channel": "rotation",
                          "time": 1,
                          "interpolation": "linear",
                          "data_points": [
                            {
                              "x": 0,
                              "y": 720,
                              "z": 0
                            }
                          ]
                        }
                      ]
                    },
                    "$LEAF": {
                      "name": "leaf",
                      "type": "bone",
                      "rotation_global": false,
                      "keyframes": [
                        {
                          "channel": "rotation",
                          "time": 0,
                          "interpolation": "linear",
                          "data_points": [
                            {
                              "x": 0,
                              "y": 0,
                              "z": 0
                            }
                          ]
                        },
                        {
                          "channel": "rotation",
                          "time": 1,
                          "interpolation": "linear",
                          "data_points": [
                            {
                              "x": 0,
                              "y": 0,
                              "z": 720
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
              ]
            }
        """.trimIndent()
    }
}
