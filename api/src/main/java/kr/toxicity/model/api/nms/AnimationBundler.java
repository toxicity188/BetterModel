/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import org.jetbrains.annotations.NotNull;

/**
 * A record that bundles animation packets for both standard clients and modded clients.
 *
 * @since 2.2.1
 * @param standard the packet bundler for standard Minecraft clients
 * @param mod      the packet bundler for clients with the specific mod enabled
 */
public record AnimationBundler(
    @NotNull PacketBundler standard,
    @NotNull ModAnimationBundler mod
) {

    /**
     * Checks if there are any animation packets to be sent.
     *
     * @since 2.2.1
     * @return true if the standard packet bundler is not empty
     */
    public boolean isNotEmpty() {
        return standard.isNotEmpty();
    }

    /**
     * Sends the appropriate animation packets to the player based on their client type.
     *
     * @since 2.2.1
     * @param handler the player's channel handler used to determine mod status and send packets
     */
    public void send(@NotNull PlayerChannelHandler handler) {
        if (handler.isModEnabled()) mod.send(handler.player());
        else standard.send(handler.player());
    }
}
