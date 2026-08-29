/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.mod.platform;

import kr.toxicity.model.api.mod.BetterModelMod;
import kr.toxicity.model.api.mod.scheduler.ModModelScheduler;
import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.platform.PlatformWorld;
import kr.toxicity.model.api.scheduler.ModelTask;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Mod location wrapped as a {@link PlatformLocation}.
 *
 * @param level the NMS level
 * @param x the x coordinate
 * @param y the y coordinate
 * @param z the z coordinate
 * @param pitch the pitch
 * @param yaw the yaw
 * @since 2.0.0
 */
public record ModLocation(@Nullable Level level, double x, double y, double z, float pitch, float yaw) implements PlatformLocation {
    @ApiStatus.Internal
    public ModLocation {
    }

    /**
     * Creates a ModLocation from the coordinates.
     *
     * @param level the NMS level
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param z     the z coordinate
     * @param pitch the pitch
     * @param yaw   the yaw
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull ModLocation of(@Nullable Level level, double x, double y, double z, float pitch, float yaw) {
        return new ModLocation(
            level,
            x,
            y,
            z,
            pitch,
            yaw
        );
    }

    /**
     * Creates a ModLocation from the coordinates with zero pitch and yaw.
     *
     * @param level the NMS level
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param z     the z coordinate
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull ModLocation of(@Nullable Level level, double x, double y, double z) {
        return new ModLocation(
            level,
            x,
            y,
            z,
            0.0f,
            0.0f
        );
    }

    /**
     * Creates a ModLocation from the position vector.
     *
     * @param level    the NMS level
     * @param position the position vector
     * @param pitch    the pitch
     * @param yaw      the yaw
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull ModLocation of(@Nullable Level level, Vec3 position, float pitch, float yaw) {
        return new ModLocation(
            level,
            position.x,
            position.y,
            position.z,
            pitch,
            yaw
        );
    }

    /**
     * Creates a ModLocation from the position vector with zero pitch and yaw.
     *
     * @param level    the NMS level
     * @param position the position vector
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull ModLocation of(@Nullable Level level, Vec3 position) {
        return new ModLocation(
            level,
            position.x,
            position.y,
            position.z,
            0.0f,
            0.0f
        );
    }

    /**
     * Creates a ModLocation from an entity's position.
     *
     * @param entity the entity
     * @return the location
     * @since 2.0.0
     */
    public static @NotNull ModLocation of(@NotNull Entity entity) {
        return new ModLocation(
            entity.level(),
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            entity.getXRot(),
            entity.getYRot()
        );
    }

    /**
     * Creates a ModLocation from an entity's eye position.
     *
     * @param entity the entity
     * @return the eye location
     * @since 2.0.0
     */
    public static @NotNull ModLocation ofEye(@NotNull Entity entity) {
        return new ModLocation(
            entity.level(),
            entity.getX(),
            entity.getEyeY(),
            entity.getZ(),
            entity.getXRot(),
            entity.getYRot()
        );
    }

    @Override
    public @NotNull PlatformWorld world() {
        if (level == null) {
            throw new IllegalStateException("level is not set");
        }

        return ModAdapter.adapt(level);
    }

    @Override
    public @NotNull PlatformLocation add(double x, double y, double z) {
        return new ModLocation(
            this.level,
            this.x + x,
            this.y + y,
            this.z + z,
            this.pitch,
            this.yaw
        );
    }

    @Override
    public @Nullable ModelTask task(@NotNull Runnable runnable) {
        return scheduler().task(runnable);
    }

    @Override
    public @Nullable ModelTask taskLater(long delay, @NotNull Runnable runnable) {
        return scheduler().taskLater(delay, runnable);
    }

    private @NotNull ModModelScheduler scheduler() {
        return BetterModelMod.platform().scheduler();
    }
}
