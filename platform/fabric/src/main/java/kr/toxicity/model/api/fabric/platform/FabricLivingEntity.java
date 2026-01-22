/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric.platform;

import kr.toxicity.model.api.platform.PlatformLivingEntity;
import kr.toxicity.model.api.platform.PlatformLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a Fabric living entity wrapped as a {@link PlatformLivingEntity}.
 *
 * @param source the source NMS living entity
 * @since 2.0.0
 */
public record FabricLivingEntity(@NotNull LivingEntity source) implements PlatformLivingEntity {
    @ApiStatus.Internal
    public FabricLivingEntity {
    }

    /**
     * Creates a FabricLivingEntity from the source.
     *
     * @param source the source living entity
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull FabricLivingEntity of(@NotNull LivingEntity source) {
        return new FabricLivingEntity(source);
    }

    @Override
    public @NotNull UUID uuid() {
        return source.getUUID();
    }

    @Override
    public @NotNull PlatformLocation location() {
        return FabricLocation.of(source);
    }

    @Override
    public @NotNull PlatformLocation eyeLocation() {
        return FabricLocation.ofEye(source);
    }
}
