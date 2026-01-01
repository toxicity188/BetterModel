/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.configuration

import kr.toxicity.model.util.DATA_FOLDER
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.ifNull
import kr.toxicity.model.util.toYaml
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

enum class PluginConfiguration(
    private val dir: String
) {
    CONFIG("config.yml"),
    ;

    fun create(): YamlConfiguration {
        val file = File(DATA_FOLDER, dir)
        val exists = file.exists()
        if (!exists) PLUGIN.saveResource(dir, false)
        val yaml = file.toYaml()
        val newYaml = PLUGIN.getResource(dir).ifNull { "Resource '$dir' not found." }.use {
            it.toYaml()
        }
        yaml.getKeys(true).forEach {
            if (!newYaml.contains(it)) yaml.set(it, null)
        }
        newYaml.getKeys(true).forEach {
            if (!yaml.contains(it)) yaml.set(it, newYaml.get(it))
            yaml.setComments(it ,newYaml.getComments(it))
        }
        return yaml.apply {
            save(file)
        }
    }
}