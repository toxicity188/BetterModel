package kr.toxicity.model.api.nms;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * An adapter of entity
 */
public interface EntityAdapter {
    /**
     * Gets a source
     * @return source entity
     */
    @NotNull Entity entity();

    /**
     * Gets vanilla entity
     * @return vanilla entity
     */
    @NotNull Object handle();

    /**
     * Checks source entity is dead
     * @return dead
     */
    boolean dead();

    /**
     * Checks source entity is on the ground
     * @return on the ground
     */
    boolean ground();

    /**
     * Checks source entity is invisible
     * @return invisible
     */
    boolean invisible();

    /**
     * Check source entity is on a glow
     * @return glow
     */
    boolean glow();

    /**
     * Check source entity is on a walk
     * @return walk
     */
    boolean onWalk();

    /**
     * Check source entity is on the fly
     * @return fly
     */
    boolean fly();

    /**
     * Gets entity's scale
     * @return scale
     */
    double scale();

    /**
     * Gets entity's pitch (x-rot)
     * @return pitch
     */
    float pitch();

    /**
     * Gets entity's body yaw (y-rot)
     * @return body yaw
     */
    float bodyYaw();

    /**
     * Gets entity's yaw (y-rot)
     * @return yaw
     */
    float yaw();

    /**
     * Gets entity's damage tick
     * @return damage tick
     */
    float damageTick();

    /**
     * Gets entity's walk speed
     * @return walk speed
     */
    float walkSpeed();

    /**
     * Gets entity's passenger point
     * @return passenger point
     */
    @NotNull Vector3f passengerPosition();
}
