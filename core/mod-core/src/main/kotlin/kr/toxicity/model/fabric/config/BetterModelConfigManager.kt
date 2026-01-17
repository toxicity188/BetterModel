/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.config

import kr.toxicity.model.fabric.logger
import kr.toxicity.model.fabric.modId
import kr.toxicity.model.util.DATA_FOLDER
import tools.jackson.databind.SerializationFeature
import tools.jackson.dataformat.yaml.YAMLMapper
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.reflect.KClass

object BetterModelConfigManager {
    private val mapper = YAMLMapper.builder()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build()

    fun <T : Any> loadOrSave(name: String, clazz: KClass<T>, defaultValue: () -> T): T {
        val path = getConfigPath(name)
        return if (path.exists()) {
            load(path, clazz).getOrThrow()
        } else {
            defaultValue()
        }.also {
            save(path, it)
        }
    }

    private fun <T : Any> load(path: Path, clazz: KClass<T>): Result<T> {
        return runCatching {
            mapper.readValue(path, clazz.java)
        }.onFailure {
            logger().error("Failed to load config at $path", it)
        }
    }

    private fun <T : Any> save(path: Path, value: T) {
        runCatching {
            path.createParentDirectories()
            mapper.writeValue(path, value)
        }.onFailure {
            logger().error("Failed to save config at $path", it)
        }
    }

    private fun getConfigPath(name: String): Path = DATA_FOLDER
        .toPath()
        .resolve(modId())
        .resolve("$name.yml")
}
