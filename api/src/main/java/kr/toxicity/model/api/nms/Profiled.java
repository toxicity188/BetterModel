/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.armor.PlayerArmor;
import kr.toxicity.model.api.player.PlayerSkinParts;
import org.jetbrains.annotations.NotNull;

/**
 * Profiled
 */
public interface Profiled {
    /**
     * Gets player game profile
     * @return game profile
     */
    @NotNull GameProfile profile();

    /**
     * Checks this player's skin is slim
     * @return slim or wide
     */
    boolean isSlim();

    /**
     * Gets player armor
     * @return armor
     */
    @NotNull PlayerArmor armors();

    /**
     * Gets player skin customization
     * @return skin parts
     */
    @NotNull PlayerSkinParts skinParts();
}
