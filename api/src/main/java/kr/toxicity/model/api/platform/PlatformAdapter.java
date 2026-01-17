/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlatformAdapter {

    int serverViewDistance();

    boolean isTickThread();

    boolean isRegionSafe();

    @Nullable PlatformPlayer player(@NotNull UUID uuid);

    @NotNull PlatformOfflinePlayer offlinePlayer(@NotNull UUID uuid);

    @NotNull PlatformItemStack air();

    @NotNull PlatformLocation zero();
}
