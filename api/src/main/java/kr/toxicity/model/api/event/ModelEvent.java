package kr.toxicity.model.api.event;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelEventBus;
import org.jetbrains.annotations.NotNull;

public interface ModelEvent {

    default @NotNull BetterModelEventBus.Result call() {
        return BetterModel.eventBus().call(this);
    }
}
