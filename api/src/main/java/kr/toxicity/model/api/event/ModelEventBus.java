package kr.toxicity.model.api.event;

import org.jetbrains.annotations.NotNull;

public interface ModelEventBus {
    void call(@NotNull ModelEvent event);
}
