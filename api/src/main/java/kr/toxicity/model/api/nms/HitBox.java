/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.mount.MountController;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Represents a hitbox for a model part, allowing for interaction and collision detection.
 * <p>
 * Hitboxes are often implemented using invisible entities (like Slimes or Interaction entities)
 * and are linked to specific bones in the model.
 * </p>
 *
 * @see org.bukkit.entity.LivingEntity
 * @since 1.15.2
 */
public interface HitBox extends Identifiable {

    /**
     * Triggers an interaction with this hitbox.
     *
     * @param player the player interacting
     * @param hand the hand used
     * @since 1.15.2
     */
    @ApiStatus.Internal
    void triggerInteract(@NotNull Player player, @NotNull ModelInteractionHand hand);

    /**
     * Triggers an interaction with this hitbox at a specific position.
     *
     * @param player the player interacting
     * @param hand the hand used
     * @param position the interaction position
     * @since 1.15.2
     */
    @ApiStatus.Internal
    void triggerInteractAt(@NotNull Player player, @NotNull ModelInteractionHand hand, @NotNull Vector position);

    /**
     * Hides this hitbox from a specific player.
     *
     * @param player the target player
     * @since 1.15.2
     */
    @ApiStatus.Internal
    void hide(@NotNull Player player);

    /**
     * Shows this hitbox to a specific player.
     *
     * @param player the target player
     * @since 1.15.2
     */
    @ApiStatus.Internal
    void show(@NotNull Player player);

    /**
     * Returns the name of the bone group associated with this hitbox.
     *
     * @return the group name
     * @since 1.15.2
     */
    @NotNull BoneName groupName();

    /**
     * Returns the mount controller for this hitbox.
     *
     * @return the mount controller
     * @since 1.15.2
     */
    @NotNull MountController mountController();

    /**
     * Sets the mount controller for this hitbox.
     *
     * @param controller the new mount controller
     * @since 1.15.2
     */
    void mountController(@NotNull MountController controller);

    /**
     * Checks if the passenger of this hitbox is walking.
     *
     * @return true if walking, false otherwise
     * @since 1.15.2
     */
    boolean onWalk();

    /**
     * Returns the source entity of this hitbox.
     *
     * @return the source entity
     * @since 1.15.2
     */
    @NotNull Entity source();

    /**
     * Mounts an entity onto this hitbox.
     *
     * @param entity the entity to mount
     * @since 1.15.2
     */
    void mount(@NotNull Entity entity);

    /**
     * Checks if this hitbox has a mount driver.
     *
     * @return true if it has a driver, false otherwise
     * @since 1.15.2
     */
    boolean hasMountDriver();

    /**
     * Checks if this hitbox is being controlled by another entity.
     *
     * @return true if controlled, false otherwise
     * @since 1.15.2
     */
    default boolean hasBeenControlled() {
        return mountController().canControl() && hasMountDriver();
    }

    /**
     * Dismounts an entity from this hitbox.
     *
     * @param entity the entity to dismount
     * @since 1.15.2
     */
    void dismount(@NotNull Entity entity);

    /**
     * Dismounts all passengers from this hitbox.
     *
     * @since 1.15.2
     */
    void dismountAll();

    /**
     * Checks if a dismount operation is forced.
     *
     * @return true if forced, false otherwise
     * @since 1.15.2
     */
    boolean forceDismount();

    /**
     * Returns the relative position of this hitbox to its source entity.
     *
     * @return the relative position
     * @since 1.15.2
     */
    @NotNull Vector3f relativePosition();

    /**
     * Removes this hitbox safely.
     * <p>
     * This method is thread-safe, unlike {@link Entity#remove()}.
     * </p>
     *
     * @since 1.15.2
     */
    void removeHitBox();

    /**
     * Returns the listener associated with this hitbox.
     *
     * @return the listener
     * @since 1.15.2
     */
    @NotNull HitBoxListener listener();

    /**
     * Returns the rendered bone that acts as the position source for this hitbox.
     *
     * @return the position source bone
     * @since 1.15.2
     */
    @NotNull RenderedBone positionSource();

    /**
     * Returns the entity tracker registry for this hitbox's source entity.
     *
     * @return an optional containing the registry, or empty if not found
     * @since 1.15.2
     */
    default @NotNull Optional<EntityTrackerRegistry> registry() {
        return BetterModel.registry(source().getUniqueId());
    }
}
