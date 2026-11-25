/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
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
import org.joml.Vector3f;

/**
 * Triggered when a player interacts with a model's hitbox at some position.
 * <p>
 * This event corresponds to a right-click interaction.
 * </p>
 *
 * @since 2.0.0
 */
@Getter
public final class HitBoxInteractAtEvent implements CancellableEvent, HitBoxEvent {

    @Setter
    private boolean cancelled;
    private final PlatformPlayer who;
    private final @NotNull HitBox hitBox;
    private final @NotNull ModelInteractionHand hand;
    private final @NotNull Vector3f position;

    /**
     * Creates a new HitBoxInteractAtEvent.
     *
     * @param who the player interacting
     * @param hitBox the hitbox being interacted with
     * @param hand the hand used for interaction
     * @param position position
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public HitBoxInteractAtEvent(@NotNull PlatformPlayer who, @NotNull HitBox hitBox, @NotNull ModelInteractionHand hand, @NotNull Vector3f position) {
        this.who = who;
        this.hitBox = hitBox;
        this.hand = hand;
        this.position = position;
    }
}
