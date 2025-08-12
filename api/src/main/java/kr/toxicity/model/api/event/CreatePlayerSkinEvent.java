package kr.toxicity.model.api.event;

import com.mojang.authlib.GameProfile;
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

    private GameProfile gameProfile;

    /**
     * Creates event
     * @param gameProfile game profile
     */
    @ApiStatus.Internal
    public CreatePlayerSkinEvent(@NotNull GameProfile gameProfile) {
        this.gameProfile = gameProfile;
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
