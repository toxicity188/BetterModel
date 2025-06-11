package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.bone.BoneName;
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
 * Gets hit-box
 * @see org.bukkit.entity.LivingEntity
 */
public interface HitBox {

    /**
     * Triggers interact with this hitbox
     * @param player player
     * @param hand hand
     */
    @ApiStatus.Internal
    void triggerInteract(@NotNull Player player, @NotNull ModelInteractionHand hand);

    /**
     * Triggers interact with this hitbox
     * @param player player
     * @param hand hand
     * @param position position
     */
    @ApiStatus.Internal
    void triggerInteractAt(@NotNull Player player, @NotNull ModelInteractionHand hand, @NotNull Vector position);

    /**
     * Hides this hitbox from player
     * @param player target
     */
    @ApiStatus.Internal
    void hide(@NotNull Player player);

    /**
     * Shows this hitbox to player
     * @param player target
     */
    @ApiStatus.Internal
    void show(@NotNull Player player);

    /**
     * Gets bone name
     * @return name
     */
    @NotNull BoneName groupName();

    /**
     * Gets mount controller
     * @return controller
     */
    @NotNull MountController mountController();

    /**
     * Sets mount controller
     * @param controller controller
     */
    void mountController(@NotNull MountController controller);

    /**
     * Checks passenger of this hit-box is on walk
     * @return on walk
     */
    boolean onWalk();

    /**
     * Gets source entity
     * @return source
     */
    @NotNull Entity source();

    /**
     * Mounts this hit-box
     * @param entity target entity
     */
    void mount(@NotNull Entity entity);

    /**
     * Gets this hit-box has a mount driver.
     * @return has a mount driver
     */
    boolean hasMountDriver();

    /**
     * Dismounts this hit-box
     * @param entity dismount
     */
    void dismount(@NotNull Entity entity);

    /**
     * Dismounts all passengers
     */
    void dismountAll();

    /**
     * Check dismount call is forced by HitBox#dismount(Entity)
     * @return force dismount
     */
    boolean forceDismount();

    /**
     * Gets relative position for source entity
     * @return relative position
     */
    @NotNull Vector3f relativePosition();

    /**
     * Removes this hit-box
     * It differs from Entity#remove because it is thread-safe
     */
    void removeHitBox();

    /**
     * Gets hit-box listener
     * @return listener
     */
    @NotNull HitBoxListener listener();

    /**
     * Gets this hitbox's tracker.
     * @return tracker
     */
    default @NotNull Optional<EntityTrackerRegistry> registry() {
        return Optional.ofNullable(EntityTrackerRegistry.registry(source().getUniqueId()));
    }
}
