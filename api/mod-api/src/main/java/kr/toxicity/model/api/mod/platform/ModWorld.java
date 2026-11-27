/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.mod.platform;

import kr.toxicity.model.api.platform.PlatformWorld;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Minecraft level wrapped as a {@link PlatformWorld}.
 *
 * @param level the source NMS level
 * @since 2.0.0
 */
public record ModWorld(@NotNull Level level) implements PlatformWorld {
    /**
     * Wraps the supplied Minecraft level. Prefer {@link #of} when adapting platform objects.
     */
    @ApiStatus.Internal
    public ModWorld {
    }

    /**
     * Wraps a Minecraft level as a platform world.
     *
     * @param level the source level
     * @return the instance
     * @since 2.0.0
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull ModWorld of(@NotNull Level level) {
        return new ModWorld(level);
    }
}
