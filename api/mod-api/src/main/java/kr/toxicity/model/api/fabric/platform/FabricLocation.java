/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric.platform;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.fabric.BetterModelFabric;
import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.platform.PlatformRegionHolder;
import kr.toxicity.model.api.platform.PlatformWorld;
import kr.toxicity.model.api.scheduler.ModelTask;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record FabricLocation(@Nullable Level level, double x, double y, double z, float pitch, float yaw) implements PlatformLocation {
    public static @NotNull FabricLocation of(@NotNull Entity entity) {
        return new FabricLocation(
            entity.level(),
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            entity.getXRot(),
            entity.getYRot()
        );
    }

    public static @NotNull FabricLocation ofEye(@NotNull Entity entity) {
        return new FabricLocation(
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

        return FabricAdapter.adapt(level);
    }

    @Override
    public @NotNull PlatformLocation add(double x, double y, double z) {
        return new FabricLocation(
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
        return regionHolder().task(runnable);
    }

    @Override
    public @Nullable ModelTask taskLater(long delay, @NotNull Runnable runnable) {
        return regionHolder().taskLater(delay, runnable);
    }

    private @NotNull PlatformRegionHolder regionHolder() {
        return ((BetterModelFabric) BetterModel.platform()).regionHolder();
    }
}
