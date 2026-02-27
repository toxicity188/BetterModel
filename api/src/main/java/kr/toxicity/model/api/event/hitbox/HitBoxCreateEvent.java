/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event.hitbox;

import kr.toxicity.model.api.nms.HitBox;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a hitbox is created.
 *
 * @param hitBox created hitbox
 * @since 2.1.0
 */
public record HitBoxCreateEvent(@NotNull HitBox hitBox) implements HitBoxEvent {

    /**
     * Returns the created hitbox.
     *
     * @return created hitbox
     * @since 2.2.0
     */
    @Override
    public @NotNull HitBox getHitBox() {
        return hitBox;
    }
}
