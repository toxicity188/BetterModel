/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.mod.platform;

import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.platform.PlatformLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a Mod entity wrapped as a {@link PlatformEntity}.
 *
 * @param source the source NMS entity
 * @since 2.0.0
 */
public record ModEntity(@NotNull Entity source) implements PlatformEntity {
    @ApiStatus.Internal
    public ModEntity {
    }

    /**
     * Creates a ModEntity from the source.
     *
     * @param source the source entity
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull ModEntity of(@NotNull Entity source) {
        return new ModEntity(source);
    }

    @Override
    public @NotNull UUID uuid() {
        return source.getUUID();
    }

    @Override
    public @NotNull PlatformLocation location() {
        return ModLocation.of(source);
    }
}
