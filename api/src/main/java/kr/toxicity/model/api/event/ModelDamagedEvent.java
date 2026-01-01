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
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A damage event of hit-box entity
 */
@Getter
@Setter
public final class ModelDamagedEvent extends EntityEvent implements Cancellable {

    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

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
        super(hitBox.source());
        this.hitBox = hitBox;
        this.source = source;
        this.damage = damage;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Gets a handler list
     * @return handler list
     */
    @SuppressWarnings("unused") //This method is necessary for event API.
    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
