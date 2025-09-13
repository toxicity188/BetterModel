package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;

public interface BoneEventHandler {
    @NotNull BoneEventDispatcher eventDispatcher();

    default void extend(@NotNull BoneEventHandler eventHandler) {
        eventDispatcher().extend(eventHandler.eventDispatcher());
    }
}
