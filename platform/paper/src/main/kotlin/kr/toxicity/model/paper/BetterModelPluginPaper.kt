/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.paper

import kr.toxicity.model.api.BetterModelPlatform
import kr.toxicity.model.bukkit.BetterModelPlugin

@Suppress("UNUSED")
class BetterModelPluginPaper : BetterModelPlugin() {

    override fun jarType(): BetterModelPlatform.JarType {
        return BetterModelPlatform.JarType.PAPER
    }
}
