/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a hitbox is synchronized.
 *
 * @param hitBox synchronized hitbox
 * @since 2.1.0
 */
public record HitBoxSyncEvent(@NotNull HitBox hitBox) implements HitBoxEvent {

    /**
     * Returns the synchronized hitbox.
     *
     * @return synchronized hitbox
     * @since 2.1.0
     */
    @Override
    public @NotNull HitBox getHitBox() {
        return hitBox;
    }
}
