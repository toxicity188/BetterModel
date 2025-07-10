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
    private final Tracker.CloseReason reason;

    /**
     * Creates event
     * @param tracker tracker
     * @param reason reason
     */
    @ApiStatus.Internal
    public CloseTrackerEvent(@NotNull Tracker tracker, @NotNull Tracker.CloseReason reason) {
        this.tracker = tracker;
        this.reason = reason;
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
