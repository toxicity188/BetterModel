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
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public final class RemovePlayerSkinEvent extends AbstractModelEvent implements Cancellable {
    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final SkinProfile skinProfile;
    private boolean cancelled;

    public RemovePlayerSkinEvent(@NotNull SkinProfile skinProfile) {
        super(true);
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
