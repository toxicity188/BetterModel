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
 * Dismounts from some model's hitbox event
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
     * Creates event
     * @param tracker tracker
     * @param bone bone
     * @param hitbox hitbox
     * @param entity entity
     */
    @ApiStatus.Internal
    public DismountModelEvent(@NotNull EntityTracker tracker, @NotNull RenderedBone bone, @NotNull HitBox hitbox, @NotNull PlatformEntity entity) {
        this.tracker = tracker;
        this.bone = bone;
        this.hitbox = hitbox;
        this.entity = entity;
    }

    /**
     * Gets entity tracker
     * @return entity tracker
     */
    public @NotNull EntityTracker tracker() {
        return tracker;
    }

    /**
     * Gets source bone
     * @return bone
     */
    public @NotNull RenderedBone bone() {
        return bone;
    }

    /**
     * Gets hitbox
     * @return hitbox
     */
    public @NotNull HitBox hitbox() {
        return hitbox;
    }

    /**
     * Gets passenger entity
     * @return entity
     */
    public PlatformEntity entity() {
        return entity;
    }
}
