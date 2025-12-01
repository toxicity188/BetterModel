/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.profile;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Profile supplier
 */
public interface ModelProfileSupplier {

    /**
     * Supplies skin
     * @param info info
     * @return uncompleted skin
     */
    @NotNull ModelProfile.Uncompleted supply(@NotNull ModelProfileInfo info);

    /**
     * Supplies skin by player
     * @param player player
     * @return uncompleted skin
     */
    default @NotNull ModelProfile.Uncompleted supply(@NotNull OfflinePlayer player) {
        return supply(new ModelProfileInfo(player.getUniqueId(), player.getName()));
    }
}
