/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlatformItemStack {

    boolean isAir();

    @NotNull PlatformItemStack enchant(boolean enchant);

    @NotNull PlatformItemStack modelData(int customModelData, @Nullable PlatformNamespace namespace);

    @NotNull PlatformItemStack clone();
}
