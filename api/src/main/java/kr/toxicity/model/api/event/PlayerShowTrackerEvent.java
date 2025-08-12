package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.Tracker;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Shows this tracker to some player
 */
@Getter
@Setter
public final class PlayerShowTrackerEvent extends AbstractPlayerModelEvent implements Cancellable {
    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final Tracker tracker;
    private boolean cancelled;

    /**
     * Creates event
     * @param tracker tracker
     * @param player player
     */
    @ApiStatus.Internal
    public PlayerShowTrackerEvent(@NotNull Tracker tracker, @NotNull Player player) {
        super(player);
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
