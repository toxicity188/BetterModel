package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.Tracker;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Closing tracker event
 */
@Getter
public final class CloseTrackerEvent extends AbstractModelEvent {

    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final Tracker tracker;

    /**
     * Creates event
     * @param tracker tracker
     */
    @ApiStatus.Internal
    public CloseTrackerEvent(@NotNull Tracker tracker) {
        this.tracker = tracker;
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
