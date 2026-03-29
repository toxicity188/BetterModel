/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric

import kr.toxicity.model.api.BetterModelLogger
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.util.logging.Logger

class BetterModelLoggerImpl : BetterModelLogger {
    private var logger: ComponentLogger? = null

    private fun logger(): ComponentLogger {
        logger?.let { logger ->
            return logger
        }

        synchronized(this) {
            logger?.let { logger ->
                return logger
            }

            return ComponentLogger.logger(LOGGER.name).also { logger = it }
        }
    }

    override fun info(vararg messages: Component) {
        val logger = logger()
        synchronized(this) {
            for (message in messages) {
                logger.info(message)
            }
        }
    }

    override fun warn(vararg messages: Component) {
        val logger = logger()
        synchronized(this) {
            for (message in messages) {
                logger.warn(message)
            }
        }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(modId())
    }
}
