/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.armor.PlayerArmor;
import kr.toxicity.model.api.player.PlayerSkinParts;
import kr.toxicity.model.api.profile.ModelProfile;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an entity that has a player profile, armor, and skin customization settings.
 * <p>
 * This interface is typically implemented by player-like entities or trackers that need to render player skins and equipment.
 * </p>
 *
 * @since 1.15.2
 */
public interface Profiled {

    /**
     * Returns the model profile (skin) of the entity.
     *
     * @return the model profile
     * @since 1.15.2
     */
    @NotNull ModelProfile profile();

    /**
     * Returns the armor equipment of the entity.
     *
     * @return the player armor
     * @since 1.15.2
     */
    @NotNull PlayerArmor armors();

    /**
     * Returns the skin customization parts (e.g., jacket, hat) of the entity.
     *
     * @return the skin parts
     * @since 1.15.2
     */
    @NotNull PlayerSkinParts skinParts();
}
