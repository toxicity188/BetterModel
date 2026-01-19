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
public class ModelInteractEvent implements CancellableEvent {

    @Setter
    private boolean cancelled;
    private final PlatformPlayer who;
    private final @NotNull HitBox hitbox;
    private final @NotNull ModelInteractionHand hand;

    /**
     * Creates a new ModelInteractEvent.
     *
     * @param who the player interacting
     * @param hitbox the hitbox being interacted with
     * @param hand the hand used for interaction
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public ModelInteractEvent(@NotNull PlatformPlayer who, @NotNull HitBox hitbox, @NotNull ModelInteractionHand hand) {
        this.who = who;
        this.hitbox = hitbox;
        this.hand = hand;
    }
}
