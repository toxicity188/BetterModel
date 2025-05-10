package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.EntityTracker;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public final class CreateEntityTrackerEvent extends CreateTrackerEvent {

    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();


    public CreateEntityTrackerEvent(@NotNull EntityTracker tracker) {
        super(tracker, false);
    }

    @NotNull
    public EntityTracker tracker() {
        return (EntityTracker) super.tracker();
    }

    @NotNull
    public Entity sourceEntity() {
        return tracker().sourceEntity();
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
