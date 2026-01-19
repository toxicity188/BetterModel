/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.platform.PlatformLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the source of damage inflicted on a model's hitbox.
 * <p>
 * This interface abstracts the platform-specific damage source details.
 * </p>
 *
 * @since 2.0.0
 */
public interface ModelDamageSource {

    /**
     * Returns the entity that caused the damage (e.g., the shooter of an arrow).
     *
     * @return the causing entity, or null if none
     * @see kr.toxicity.model.api.platform.PlatformLivingEntity
     * @since 2.0.0
     */
    @Nullable PlatformEntity getCausingEntity();

    /**
     * Returns the entity that directly inflicted the damage (e.g., the arrow itself).
     *
     * @return the direct entity, or null if none
     * @since 2.0.0
     */
    @Nullable PlatformEntity getDirectEntity();

    /**
     * Returns the location where the damage occurred.
     *
     * @return the damage location, or null if unknown
     * @since 2.0.0
     */
    @Nullable PlatformLocation getDamageLocation();

    /**
     * Returns the location of the damage source.
     *
     * @return the source location, or null if unknown
     * @since 2.0.0
     */
    @Nullable PlatformLocation getSourceLocation();

    /**
     * Checks if the damage was indirect (e.g., projectile).
     *
     * @return true if indirect, false otherwise
     * @since 2.0.0
     */
    boolean isIndirect();

    /**
     * Returns the amount of food exhaustion caused by this damage.
     *
     * @return the food exhaustion
     * @since 2.0.0
     */
    float getFoodExhaustion();

    /**
     * Checks if this damage should be scaled based on difficulty.
     *
     * @return true if scalable, false otherwise
     * @since 2.0.0
     */
    boolean scalesWithDifficulty();
}
