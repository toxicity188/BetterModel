/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event.hitbox;

import kr.toxicity.model.api.event.ModelEvent;
import kr.toxicity.model.api.nms.HitBox;
import org.jetbrains.annotations.NotNull;

/**
 * Base contract for events associated with a {@link HitBox}.
 *
 * @since 2.1.0
 */
public interface HitBoxEvent extends ModelEvent {

    /**
     * Returns the target hitbox of this event.
     *
     * @return target hitbox
     * @since 2.1.0
     */
    @NotNull HitBox getHitBox();
}

