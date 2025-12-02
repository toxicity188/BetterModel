/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import kr.toxicity.model.api.event.AnimationSignalEvent
import kr.toxicity.model.api.manager.ScriptManager
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.script.ScriptBuilder
import kr.toxicity.model.script.*
import kr.toxicity.model.util.boneName
import kr.toxicity.model.util.bonePredicate
import kr.toxicity.model.util.call
import java.util.regex.Matcher
import java.util.regex.Pattern

object ScriptManagerImpl : ScriptManager, GlobalManager {

    private val scriptMap = hashMapOf<String, ScriptBuilder>()
    private val scriptPattern = Pattern.compile("^(?<name>[a-zA-Z]+)(:(?<argument>(\\w|_|-)+))?(\\{(?<metadata>(\\w|\\W)+)})?$")
    private val validatePattern = Pattern.compile("^[a-z]+$")

    init {
        addBuilder("signal") {
            val args = it.args() ?: return@addBuilder AnimationScript.EMPTY
            AnimationScript.of { tracker ->
                tracker.pipeline.allPlayer().forEach { player ->
                    AnimationSignalEvent(player, args).call()
                }
            }
        }

        addBuilder("tint") {
            TintScript(
                it.metadata.bonePredicate,
                it.metadata.asNumber("color")?.toInt() ?: return@addBuilder AnimationScript.EMPTY,
                it.metadata.asBoolean("damage") == true
            )
        }
        addBuilder("partvis") {
            PartVisibilityScript(
                it.metadata.bonePredicate,
                it.metadata.asBoolean("visible") ?: return@addBuilder AnimationScript.EMPTY,
            )
        }
        addBuilder("partbright") {
            BrightnessScript(
                it.metadata.bonePredicate,
                it.metadata().asNumber("block")?.toInt() ?: return@addBuilder AnimationScript.EMPTY,
                it.metadata().asNumber("sky")?.toInt() ?: return@addBuilder AnimationScript.EMPTY,
            )
        }
        addBuilder("enchant") {
            EnchantScript(
                it.metadata.bonePredicate,
                it.metadata.asBoolean("enchant") ?: return@addBuilder AnimationScript.EMPTY,
            )
        }
        addBuilder("changepart") {
            ChangePartScript(
                it.metadata.bonePredicate,
                it.metadata.asString("nmodel") ?: return@addBuilder AnimationScript.EMPTY,
                it.metadata.asString("npart")?.boneName ?: return@addBuilder AnimationScript.EMPTY
            )
        }
        addBuilder("remap") {
            RemapScript(
                it.metadata.asString("model") ?: return@addBuilder AnimationScript.EMPTY,
                it.metadata.asString("map")
            )
        }
    }

    override fun build(script: String): AnimationScript? = script.toScript()

    override fun addBuilder(name: String, script: ScriptBuilder) {
        if (!validatePattern.matcher(name).find()) throw RuntimeException("name must be in [a-z]")
        scriptMap[name] = script
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
    }

    private fun String.toScript(): AnimationScript? = scriptPattern.matcher(this)
        .takeIf(Matcher::find)
        ?.let {
            scriptMap[it.group("name").lowercase()]?.build(ScriptBuilder.ScriptData(it.group("argument"),
                ScriptMetaDataImpl(it.group("metadata")
                    ?.split(';')
                    ?.associate { pair ->
                        pair.split('=', limit = 2).let { arr -> arr[0] to arr[1] }
                    }
                    ?: emptyMap()
                )
            ))
        }

    private class ScriptMetaDataImpl(
        private val map: Map<String, String>
    ) : ScriptBuilder.ScriptMetaData {
        override fun toMap(): Map<String, String> = map
    }
}
