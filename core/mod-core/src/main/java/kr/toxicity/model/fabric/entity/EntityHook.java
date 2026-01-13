/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.entity;

import org.jetbrains.annotations.Nullable;

public interface EntityHook {
    @Nullable String bettermodel$getModelData();

    void bettermodel$setModelData(@Nullable String modelData);
}
