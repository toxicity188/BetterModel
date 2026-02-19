/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event.hitbox;

import kr.toxicity.model.api.event.CancellableEvent;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.ModelInteractionHand;
import kr.toxicity.model.api.platform.PlatformPlayer;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a player interacts with a model's hitbox.
 * <p>
 * This event corresponds to a right-click interaction.
 * </p>
 *
 * @since 2.0.0
 */
@Getter
public class HitBoxInteractEvent implements CancellableEvent, HitBoxEvent {

    @Setter
    private boolean cancelled;
    private final PlatformPlayer who;
    private final @NotNull HitBox hitBox;
    private final @NotNull ModelInteractionHand hand;

    /**
     * Creates a new HitBoxInteractEvent.
     *
     * @param who the player interacting
     * @param hitBox the hitbox being interacted with
     * @param hand the hand used for interaction
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public HitBoxInteractEvent(@NotNull PlatformPlayer who, @NotNull HitBox hitBox, @NotNull ModelInteractionHand hand) {
        this.who = who;
        this.hitBox = hitBox;
        this.hand = hand;
    }
}
