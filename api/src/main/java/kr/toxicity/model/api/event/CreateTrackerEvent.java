package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.Tracker;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Tracker create event
 */
@Getter
public abstract class CreateTrackerEvent extends AbstractModelEvent {
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
    public CreateTrackerEvent(@NotNull Tracker tracker) {
        super();
        this.tracker = tracker;
    }

    /**
     * Creates event
     * @param tracker tracker
     * @param async async
     */
    @ApiStatus.Internal
    public CreateTrackerEvent(@NotNull Tracker tracker, boolean async) {
        super(async);
        this.tracker = tracker;
    }

    /**
     * Gets tracker
     * @return tracker
     */
    public @NotNull Tracker tracker() {
        return tracker;
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
