package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.DummyTracker;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Create event of fake tracker
 */
@Getter
public final class CreateDummyTrackerEvent extends CreateTrackerEvent {

    /**
     * Creates event
     * @param tracker tracker
     */
    @ApiStatus.Internal
    public CreateDummyTrackerEvent(@NotNull DummyTracker tracker) {
        super(tracker);
    }

    @NotNull
    public DummyTracker tracker() {
        return (DummyTracker) super.tracker();
    }
}
