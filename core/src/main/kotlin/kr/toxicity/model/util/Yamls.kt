/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.util

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

fun File.toYaml() = YamlConfiguration.loadConfiguration(this)
fun InputStream.toYaml() = InputStreamReader(this, StandardCharsets.UTF_8).use { reader ->
    reader.buffered().use(YamlConfiguration::loadConfiguration)
}