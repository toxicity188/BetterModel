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


public final class BukkitPlayer extends BukkitLivingEntity implements PlatformPlayer {

    public BukkitPlayer(@NotNull Player source) {
        super(source);
    }

    public @NotNull Player source() {
        return (Player) super.source();
    }

    @Override
    public @NotNull String name() {
        return source().getName();
    }
}
