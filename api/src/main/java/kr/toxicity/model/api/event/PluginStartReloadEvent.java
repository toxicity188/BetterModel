package kr.toxicity.model.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PluginStartReloadEvent extends AbstractModelEvent {
    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

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
