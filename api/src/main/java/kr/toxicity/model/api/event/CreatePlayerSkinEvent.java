/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.skin.SkinProfile;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Create player skin data event
 */
@Getter
@Setter
public final class CreatePlayerSkinEvent extends AbstractModelEvent {
    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private SkinProfile skinProfile;

    /**
     * Creates event
     * @param skinProfile game profile
     */
    @ApiStatus.Internal
    public CreatePlayerSkinEvent(@NotNull SkinProfile skinProfile) {
        this.skinProfile = skinProfile;
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
