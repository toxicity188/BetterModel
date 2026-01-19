/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a model's hitbox is damaged.
 * <p>
 * This event allows modifying the damage amount or cancelling the damage entirely.
 * </p>
 *
 * @since 2.0.0
 */
@Getter
@Setter
public final class ModelDamagedEvent implements CancellableEvent {

    private final @NotNull HitBox hitBox;
    private final ModelDamageSource source;

    private float damage;
    private boolean cancelled;

    /**
     * Creates a new ModelDamagedEvent.
     *
     * @param hitBox the hitbox being damaged
     * @param source the source of the damage
     * @param damage the amount of damage
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public ModelDamagedEvent(@NotNull HitBox hitBox, @NotNull ModelDamageSource source, float damage) {
        this.hitBox = hitBox;
        this.source = source;
        this.damage = damage;
    }
}
