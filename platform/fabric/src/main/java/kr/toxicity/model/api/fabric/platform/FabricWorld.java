/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric.platform;

import kr.toxicity.model.api.platform.PlatformWorld;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Fabric world wrapped as a {@link PlatformWorld}.
 *
 * @param level the source NMS level
 * @since 2.0.0
 */
public record FabricWorld(@NotNull Level level) implements PlatformWorld {
    @ApiStatus.Internal
    public FabricWorld {
    }

    /**
     * Creates a FabricWorld from the level.
     *
     * @param level the source level
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull FabricWorld of(@NotNull Level level) {
        return new FabricWorld(level);
    }
}
