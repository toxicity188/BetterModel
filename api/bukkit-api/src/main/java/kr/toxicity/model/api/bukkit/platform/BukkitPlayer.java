/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.platform;

import kr.toxicity.model.api.platform.PlatformPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


/**
 * Represents a Bukkit player wrapped as a {@link PlatformPlayer}.
 *
 * @since 2.0.0
 */
public final class BukkitPlayer extends BukkitLivingEntity implements PlatformPlayer {

    /**
     * Creates a new BukkitPlayer wrapper.
     *
     * @param source the source Bukkit player
     * @since 2.0.0
     */
    public BukkitPlayer(@NotNull Player source) {
        super(source);
    }

    /**
     * Returns the underlying Bukkit player.
     *
     * @return the source player
     * @since 2.0.0
     */
    public @NotNull Player source() {
        return (Player) super.source();
    }

    @Override
    public @NotNull String name() {
        return source().getName();
    }
}
