/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager.debug

import kr.toxicity.model.manager.ReloadPipeline

interface ReloadIndicator {
    infix fun status(status: ReloadPipeline.Status)
    fun close()
}