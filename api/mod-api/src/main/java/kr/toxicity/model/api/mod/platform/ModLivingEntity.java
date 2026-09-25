/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.mod.platform;

import kr.toxicity.model.api.platform.PlatformLivingEntity;
import kr.toxicity.model.api.platform.PlatformLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a Mod living entity wrapped as a {@link PlatformLivingEntity}.
 *
 * @param source the source NMS living entity
 * @since 2.0.0
 */
public record ModLivingEntity(@NotNull LivingEntity source) implements PlatformLivingEntity {
    @ApiStatus.Internal
    public ModLivingEntity {
    }

    /**
     * Creates a ModLivingEntity from the source.
     *
     * @param source the source living entity
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull ModLivingEntity of(@NotNull LivingEntity source) {
        return new ModLivingEntity(source);
    }

    @Override
    public @NotNull UUID uuid() {
        return source.getUUID();
    }

    @Override
    public @NotNull PlatformLocation location() {
        return ModLocation.of(source);
    }

    @Override
    public @NotNull PlatformLocation eyeLocation() {
        return ModLocation.ofEye(source);
    }
}
