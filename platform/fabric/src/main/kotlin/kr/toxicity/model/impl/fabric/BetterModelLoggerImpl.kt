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

    private val logger by lazy {
        ComponentLogger.logger(LOGGER.name)
    }

    @Synchronized
    override fun info(vararg messages: Component) {
        messages.forEach(logger::info)
    }

    @Synchronized
    override fun warn(vararg messages: Component) {
        messages.forEach(logger::warn)
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(modId())
    }
}
