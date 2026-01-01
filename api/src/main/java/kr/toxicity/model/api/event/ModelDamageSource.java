/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * A damage source of hit-box
 */
public interface ModelDamageSource {

    /**
     * Gets causing entity
     * @see org.bukkit.entity.LivingEntity
     * @return causing entity
     */
    @Nullable Entity getCausingEntity();

    /**
     * Gets direct like
     * @see org.bukkit.entity.Projectile
     * @return direct entity
     */
    @Nullable Entity getDirectEntity();

    /**
     * Gets damage location
     * @return damage location
     */
    @Nullable Location getDamageLocation();

    /**
     * Gets source location
     * @return source location
     */
    @Nullable Location getSourceLocation();

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
