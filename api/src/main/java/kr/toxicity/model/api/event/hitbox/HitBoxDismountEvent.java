/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.event.hitbox;

import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.platform.PlatformEntity;
import org.jetbrains.annotations.NotNull;

/**
 * An event called when an entity dismounts from a hit box.
 *
 * @param hitBox the hit box
 * @param entity the entity
 * @since 2.1.0
 */
public record HitBoxDismountEvent(@NotNull HitBox hitBox, @NotNull PlatformEntity entity) implements HitBoxEvent {
    @Override
    public @NotNull HitBox getHitBox() {
        return hitBox;
    }
}
