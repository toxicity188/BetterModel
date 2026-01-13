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
 * A damage source of hit-box
 */
public interface ModelDamageSource {

    /**
     * Gets causing entity
     * @see kr.toxicity.model.api.platform.PlatformLivingEntity
     * @return causing entity
     */
    @Nullable PlatformEntity getCausingEntity();

    /**
     * Gets direct like
     * @return direct entity
     */
    @Nullable PlatformEntity getDirectEntity();

    /**
     * Gets damage location
     * @return damage location
     */
    @Nullable PlatformLocation getDamageLocation();

    /**
     * Gets source location
     * @return source location
     */
    @Nullable PlatformLocation getSourceLocation();

    /**
     * Checks causing entity is not equals with direct entity
     * @return is indirect
     */
    boolean isIndirect();

    /**
     * Gets food exhaustion
     * @return food exhaustion
     */
    float getFoodExhaustion();

    /**
     * Checks this damage should be scaled by difficulty
     * @return should be scaled
     */
    boolean scalesWithDifficulty();
}
