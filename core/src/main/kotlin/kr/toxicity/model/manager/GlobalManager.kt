/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import kr.toxicity.model.api.pack.PackZipper

interface GlobalManager {
    fun start() {}
    fun reload(pipeline: ReloadPipeline, zipper: PackZipper)
    fun end() {}
}