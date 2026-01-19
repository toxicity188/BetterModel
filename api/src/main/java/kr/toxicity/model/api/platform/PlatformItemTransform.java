/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

/**
 * Defines the display context for an item model.
 * <p>
 * These values correspond to the display settings in a Minecraft item model file.
 * </p>
 *
 * @since 2.0.0
 */
public enum PlatformItemTransform {
    /**
     * No specific transform.
     * @since 2.0.0
     */
    NONE,
    /**
     * Displayed in the left hand in third-person view.
     * @since 2.0.0
     */
    THIRDPERSON_LEFTHAND,
    /**
     * Displayed in the right hand in third-person view.
     * @since 2.0.0
     */
    THIRDPERSON_RIGHTHAND,
    /**
     * Displayed in the left hand in first-person view.
     * @since 2.0.0
     */
    FIRSTPERSON_LEFTHAND,
    /**
     * Displayed in the right hand in first-person view.
     * @since 2.0.0
     */
    FIRSTPERSON_RIGHTHAND,
    /**
     * Displayed on the head (e.g., helmet).
     * @since 2.0.0
     */
    HEAD,
    /**
     * Displayed in a GUI slot.
     * @since 2.0.0
     */
    GUI,
    /**
     * Displayed on the ground as an item entity.
     * @since 2.0.0
     */
    GROUND,
    /**
     * Displayed in an item frame.
     * @since 2.0.0
     */
    FIXED
}
