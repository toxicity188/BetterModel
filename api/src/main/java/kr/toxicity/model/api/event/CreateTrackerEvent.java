package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.Tracker;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class CreateTrackerEvent extends AbstractModelEvent {

    private final Tracker tracker;

    public CreateTrackerEvent(@NotNull Tracker tracker) {
        super();
        this.tracker = tracker;
    }

    public CreateTrackerEvent(@NotNull Tracker tracker, boolean async) {
        super(async);
        this.tracker = tracker;
    }

    public @NotNull Tracker tracker() {
        return tracker;
    }
}
