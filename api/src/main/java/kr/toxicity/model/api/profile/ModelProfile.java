/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.profile;

import kr.toxicity.model.api.BetterModel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Model skin
 */
public interface ModelProfile {

    /**
     * Unknown skin
     */
    ModelProfile UNKNOWN = of(ModelProfileInfo.UNKNOWN);

    /**
     * Creates profile
     * @param info info
     * @return profile
     */
    static @NotNull ModelProfile of(@NotNull ModelProfileInfo info) {
        return new Simple(info, ModelProfileSkin.EMPTY);
    }

    /**
     * Creates profile
     * @param info info
     * @param skin skin
     * @return profile
     */
    static @NotNull ModelProfile of(@NotNull ModelProfileInfo info, @NotNull ModelProfileSkin skin) {
        return new Simple(info, skin);
    }

    /**
     * Gets skin by player
     * @param player player
     * @return model skin
     */
    static @NotNull ModelProfile of(@NotNull Player player) {
        var channel = BetterModel.plugin().playerManager().player(player.getUniqueId());
        return channel != null ? channel.profile() : BetterModel.nms().profile(player);
    }

    /**
     * Gets uncompleted skin by offline player
     * @param offlinePlayer offline player
     * @return skin
     */
    static @NotNull Uncompleted of(@NotNull OfflinePlayer offlinePlayer) {
        return BetterModel.plugin().profileManager().supplier().supply(offlinePlayer);
    }

    /**
     * Gets uncompleted skin by offline player's uuid
     * @param uuid offline player's uuid
     * @return skin
     */
    static @NotNull Uncompleted of(@NotNull UUID uuid) {
        return of(Bukkit.getOfflinePlayer(uuid));
    }

    /**
     * Gets info
     * @return info
     */
    @NotNull ModelProfileInfo info();

    /**
     * Gets skin
     * @return skin
     */
    @NotNull ModelProfileSkin skin();


    /**
     * Makes this skin as uncompleted
     * @return uncompleted skin
     */
    default @NotNull Uncompleted asUncompleted() {
        return new Uncompleted() {
            @Override
            public @NotNull ModelProfileInfo info() {
                return ModelProfile.this.info();
            }

            @Override
            public @NotNull CompletableFuture<ModelProfile> complete() {
                return CompletableFuture.completedFuture(ModelProfile.this);
            }
        };
    }

    /**
     * Gets player
     * @return player
     */
    default @Nullable Player player() {
        return Bukkit.getPlayer(info().id());
    }

    /**
     * Simple profile
     * @param info info
     * @param skin skin
     */
    record Simple(@NotNull ModelProfileInfo info, @NotNull ModelProfileSkin skin) implements ModelProfile {
    }

    /**
     * Uncompleted skin
     */
    interface Uncompleted {

        /**
         * Gets info
         * @return info
         */
        @NotNull ModelProfileInfo info();

        /**
         * Completes profile
         * @return completed profile
         */
        @NotNull CompletableFuture<ModelProfile> complete();

        /**
         * Gets fallback profile
         * @return profile
         */
        default @NotNull ModelProfile fallback() {
            return of(info());
        }
    }
}
