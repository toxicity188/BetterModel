package kr.toxicity.model.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PluginStartReloadEvent extends AbstractModelEvent {
    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Gets handler list
     * @return handler list
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
