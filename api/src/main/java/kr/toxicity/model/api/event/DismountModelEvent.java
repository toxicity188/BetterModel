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
 * Triggered when an entity dismounts from a model's hitbox.
 * <p>
 * This event allows plugins/mods to intercept and potentially cancel the dismounting process.
 * </p>
 *
 * @since 2.0.0
 */
public final class DismountModelEvent implements CancellableEvent {

    private final EntityTracker tracker;
    private final RenderedBone bone;
    private final HitBox hitbox;
    private final PlatformEntity entity;
    @Getter
    @Setter
    private boolean cancelled;

    /**
     * Creates a new DismountModelEvent.
     *
     * @param tracker the entity tracker associated with the model
     * @param bone the bone associated with the hitbox
     * @param hitbox the hitbox being dismounted
     * @param entity the entity dismounting
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public DismountModelEvent(@NotNull EntityTracker tracker, @NotNull RenderedBone bone, @NotNull HitBox hitbox, @NotNull PlatformEntity entity) {
        this.tracker = tracker;
        this.bone = bone;
        this.hitbox = hitbox;
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
     * Returns the hitbox being dismounted.
     *
     * @return the hitbox
     * @since 2.0.0
     */
    public @NotNull HitBox hitbox() {
        return hitbox;
    }

    /**
     * Returns the entity dismounting the hitbox.
     *
     * @return the passenger entity
     * @since 2.0.0
     */
    public PlatformEntity entity() {
        return entity;
    }
}
