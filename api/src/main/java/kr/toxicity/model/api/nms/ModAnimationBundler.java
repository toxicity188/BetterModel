/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.platform.PlatformPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * A bundler that sends animation data to a player.
 * @since 2.2.1
 */
public interface ModAnimationBundler {

    /**
     * Sends the bundled animation data to the specified channel.
     *
     * @since 3.6.0
     * @param channel the channel to receive the animation
     */
    default void send(@NotNull PlayerChannelHandler channel) {
        send(channel.player());
    }

    /**
     * Sends the bundled animation data to the specified player.
     *
     * @since 2.2.1
     * @param player the player to receive the animation
     */
    void send(@NotNull PlatformPlayer player);
}
