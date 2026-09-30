/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.armor;

import kr.toxicity.model.api.util.TransformedItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Player armor
 */
public interface PlayerArmor {

    /**
     * Empty armor
     */
    PlayerArmor EMPTY = new PlayerArmor() {
        @Override
        public @Nullable ArmorItem helmet() {
            return null;
        }

        @Override
        public @Nullable ArmorItem chestplate() {
            return null;
        }

        @Override
        public @Nullable ArmorItem leggings() {
            return null;
        }

        @Override
        public @Nullable ArmorItem boots() {
            return null;
        }
    };

    /**
     * Gets helmet
     * @return helmet
     */
    @Nullable ArmorItem helmet();

    /**
     * Gets the custom item-model stack worn in the helmet slot.
     * <p>
     * A {@code null} value means the helmet should use BetterModel's generated
     * vanilla armor layer. For example:
     * <pre>{@code
     * TransformedItemStack customHelmet = armor.helmetItem();
     * if (customHelmet != null) {
     *     // Render the original item stack with its custom item model.
     * }
     * }</pre>
     *
     * @return the custom helmet item, or {@code null} for vanilla armor rendering
     * @since 3.4.1
     */
    default @Nullable TransformedItemStack helmetItem() {
        return null;
    }

    /**
     * Gets chestplate
     * @return chestplate
     */
    @Nullable ArmorItem chestplate();

    /**
     * Gets leggings
     * @return leggings
     */
    @Nullable ArmorItem leggings();

    /**
     * Gets boots
     * @return boots
     */
    @Nullable ArmorItem boots();
}
