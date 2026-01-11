/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.ModelInteractionHand;
import kr.toxicity.model.api.platform.PlatformPlayer;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * An interacted event of damage event
 */
public record ModelInteractAtEvent(
    @NotNull PlatformPlayer who,
    @NotNull HitBox hitbox,
    @NotNull ModelInteractionHand hand,
    @NotNull Vector3f position
) implements ModelEvent {
}
