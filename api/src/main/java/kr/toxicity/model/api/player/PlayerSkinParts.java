/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.player;

/**
 * Player skin parts
 * @param bitmask bit mask
 */
public record PlayerSkinParts(int bitmask) {
    /**
     * The default skin parts configuration where all parts (Cape, Jacket, Sleeves, Pants, Hat) are visible.
     */
    public static final PlayerSkinParts DEFAULT = new PlayerSkinParts(0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40);

    /**
     * Checks if the 'Cape' part of the player's skin is enabled (visible).
     *
     * @return {@code true} if the cape is enabled, {@code false} otherwise.
     */
    public boolean isCapeEnabled() {
        return (bitmask & 0x01) != 0;
    }

    /**
     * Checks if the 'Jacket' part of the player's skin is enabled (visible).
     *
     * @return {@code true} if the jacket is enabled, {@code false} otherwise.
     */
    public boolean isJacketEnabled() {
        return (bitmask & 0x02) != 0;
    }

    /**
     * Checks if the 'Left Sleeve' part of the player's skin is enabled (visible).
     *
     * @return {@code true} if the left sleeve is enabled, {@code false} otherwise.
     */
    public boolean isLeftSleeveEnabled() {
        return (bitmask & 0x04) != 0;
    }

    /**
     * Checks if the 'Right Sleeve' part of the player's skin is enabled (visible).
     *
     * @return {@code true} if the right sleeve is enabled, {@code false} otherwise.
     */
    public boolean isRightSleeveEnabled() {
        return (bitmask & 0x08) != 0;
    }

    /**
     * Checks if the 'Left Pants Leg' part of the player's skin is enabled (visible).
     *
     * @return {@code true} if the left pants leg is enabled, {@code false} otherwise.
     */
    public boolean isLeftPantsEnabled() {
        return (bitmask & 0x10) != 0;
    }

    /**
     * Checks if the 'Right Pants Leg' part of the player's skin is enabled (visible).
     *
     * @return {@code true} if the right pants leg is enabled, {@code false} otherwise.
     */
    public boolean isRightPantsEnabled() {
        return (bitmask & 0x20) != 0;
    }

    /**
     * Checks if the 'Hat' (or head overlay) part of the player's skin is enabled (visible).
     *
     * @return {@code true} if the hat is enabled, {@code false} otherwise.
     */
    public boolean isHatEnabled() {
        return (bitmask & 0x40) != 0;
    }
}
