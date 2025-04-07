package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.Tracker;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public final class CreateTrackerEvent extends AbstractModelEvent {

    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final Tracker tracker;

    public CreateTrackerEvent(@NotNull Tracker tracker) {
        this.tracker = tracker;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
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
