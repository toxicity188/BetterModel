/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.tracker.EntityTracker;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when an entity mounts a model's hitbox.
 * <p>
 * This event allows plugins/mods to intercept and potentially cancel the mounting process.
 * </p>
 *
 * @since 2.0.0
 */
public final class MountModelEvent implements CancellableEvent {

    private final EntityTracker tracker;
    private final RenderedBone bone;
    private final HitBox hitBox;
    private final PlatformEntity entity;
    @Getter
    @Setter
    private boolean cancelled;

    /**
     * Creates a new MountModelEvent.
     *
     * @param tracker the entity tracker associated with the model
     * @param bone the bone associated with the hitbox
     * @param hitBox the hitbox being mounted
     * @param entity the entity attempting to mount
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public MountModelEvent(@NotNull EntityTracker tracker, @NotNull RenderedBone bone, @NotNull HitBox hitBox, @NotNull PlatformEntity entity) {
        this.tracker = tracker;
        this.bone = bone;
        this.hitBox = hitBox;
        this.entity = entity;
    }

    /**
     * Returns the entity tracker associated with the model.
     *
     * @return the entity tracker
     * @since 2.0.0
     */
    public @NotNull EntityTracker tracker() {
        return tracker;
    }

    /**
     * Returns the bone associated with the hitbox.
     *
     * @return the rendered bone
     * @since 2.0.0
     */
    public @NotNull RenderedBone bone() {
        return bone;
    }

    /**
     * Returns the hitbox being mounted.
     *
     * @return the hitbox
     * @since 2.0.0
     */
    public @NotNull HitBox hitbox() {
        return hitBox;
    }

    /**
     * Returns the entity attempting to mount the hitbox.
     *
     * @return the passenger entity
     * @since 2.0.0
     */
    public PlatformEntity entity() {
        return entity;
    }
}
