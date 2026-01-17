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
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record FabricLivingEntity(@NotNull LivingEntity source) implements PlatformLivingEntity {
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
