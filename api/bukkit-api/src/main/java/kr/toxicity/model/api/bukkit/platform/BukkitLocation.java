/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.platform;

import kr.toxicity.model.api.bukkit.BetterModelBukkit;
import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.platform.PlatformWorld;
import kr.toxicity.model.api.scheduler.ModelTask;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Bukkit location wrapped as a {@link PlatformLocation}.
 *
 * @param source the source Bukkit location
 * @since 2.0.0
 */
public record BukkitLocation(@NotNull Location source) implements PlatformLocation {

    @Override
    public @NotNull PlatformWorld world() {
        return BukkitAdapter.adapt(source.getWorld());
    }

    @Override
    public double x() {
        return source.getX();
    }

    @Override
    public double y() {
        return source.getY();
    }

    @Override
    public double z() {
        return source.getZ();
    }

    @Override
    public float pitch() {
        return source.getPitch();
    }

    @Override
    public float yaw() {
        return source.getYaw();
    }

    @Override
    public @NotNull PlatformLocation add(double x, double y, double z) {
        return BukkitAdapter.adapt(source.clone().add(x, y, z));
    }

    @Override
    public @Nullable ModelTask task(@NotNull Runnable runnable) {
        return BetterModelBukkit.platform().scheduler().task(source, runnable);
    }

    @Override
    public @Nullable ModelTask taskLater(long delay, @NotNull Runnable runnable) {
        return BetterModelBukkit.platform().scheduler().taskLater(source, delay, runnable);
    }
}
