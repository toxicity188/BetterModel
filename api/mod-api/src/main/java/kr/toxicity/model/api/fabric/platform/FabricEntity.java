/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric.platform;

import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.platform.PlatformLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record FabricEntity(@NotNull Entity entity) implements PlatformEntity {
    @Override
    public @NotNull UUID uuid() {
        return entity.getUUID();
    }

    @Override
    public @NotNull PlatformLocation location() {
        return FabricLocation.of(entity);
    }
}
