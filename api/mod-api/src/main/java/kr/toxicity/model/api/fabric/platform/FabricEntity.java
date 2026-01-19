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

/**
 * Represents a Fabric entity wrapped as a {@link PlatformEntity}.
 *
 * @param source the source NMS entity
 * @since 2.0.0
 */
public record FabricEntity(@NotNull Entity source) implements PlatformEntity {
    @Override
    public @NotNull UUID uuid() {
        return source.getUUID();
    }

    @Override
    public @NotNull PlatformLocation location() {
        return FabricLocation.of(source);
    }
}
