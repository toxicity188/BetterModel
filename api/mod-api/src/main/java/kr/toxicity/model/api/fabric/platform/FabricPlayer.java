/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric.platform;

import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.platform.PlatformPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record FabricPlayer(@NotNull ServerPlayer player) implements PlatformPlayer {
    @Override
    public @NotNull UUID uuid() {
        return player.getUUID();
    }

    @Override
    public @NotNull PlatformLocation location() {
        return FabricLocation.of(player);
    }

    @Override
    public @NotNull PlatformLocation eyeLocation() {
        return FabricLocation.ofEye(player);
    }

    @Override
    public @NotNull String name() {
        return player.getPlainTextName();
    }
}
