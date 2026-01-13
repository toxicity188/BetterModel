/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric;

import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.platform.PlatformRegionHolder;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public interface BetterModelFabric extends BetterModelPlatform {
    @NotNull MinecraftServer server();

    @NotNull PlatformRegionHolder regionHolder();
}
