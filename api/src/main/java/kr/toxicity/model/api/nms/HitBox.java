package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.mount.MountController;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Gets hit-box
 * @see org.bukkit.entity.LivingEntity
 */
public interface HitBox {
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
}
