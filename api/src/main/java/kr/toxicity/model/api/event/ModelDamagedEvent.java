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
 * A damage event of hit-box entity
 */
@Getter
@Setter
public final class ModelDamagedEvent implements CancellableEvent {

    private final @NotNull HitBox hitBox;
    private final ModelDamageSource source;

    private float damage;
    private boolean cancelled;

    /**
     * Creates damage event
     * @param hitBox hit-box
     * @param source source
     * @param damage damage amount
     */
    @ApiStatus.Internal
    public ModelDamagedEvent(@NotNull HitBox hitBox, @NotNull ModelDamageSource source, float damage) {
        this.hitBox = hitBox;
        this.source = source;
        this.damage = damage;
    }
}
