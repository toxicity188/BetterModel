/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.ModelInteractionHand;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * An interacted event of damage event
 */
@Getter
public final class ModelInteractAtEvent extends ModelInteractEvent {

    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final Vector position;

    /**
     * Creates interact event
     * @param who player
     * @param hitBox hit-box
     * @param hand interacted hand
     * @param position position
     */
    @ApiStatus.Internal
    public ModelInteractAtEvent(@NotNull Player who, @NotNull HitBox hitBox, @NotNull ModelInteractionHand hand, @NotNull Vector position) {
        super(who, hitBox, hand);
        this.position = position;
    }


    @Override
    public @NotNull HandlerList getHandlers() {
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
