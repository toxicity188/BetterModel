/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.mod.platform;

import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.platform.PlatformPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a Mod player wrapped as a {@link PlatformPlayer}.
 *
 * @param source the source NMS player connection
 * @since 2.0.0
 */
public record ModPlayer(@NotNull ServerPlayerConnection source) implements PlatformPlayer {
    @ApiStatus.Internal
    public ModPlayer {
    }

    /**
     * Creates a ModPlayer from the source.
     *
     * @param source the source player connection
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull ModPlayer of(@NotNull ServerPlayerConnection source) {
        return new ModPlayer(source);
    }

    @Override
    public @NotNull UUID uuid() {
        return source.getPlayer().getUUID();
    }

    @Override
    public @NotNull PlatformLocation location() {
        return ModLocation.of(source.getPlayer());
    }

    @Override
    public @NotNull PlatformLocation eyeLocation() {
        return ModLocation.ofEye(source.getPlayer());
    }

    @Override
    public @NotNull String name() {
        return source.getPlayer().getPlainTextName();
    }
}
