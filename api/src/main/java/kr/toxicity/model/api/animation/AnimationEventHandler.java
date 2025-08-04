package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class AnimationEventHandler {

    private final AtomicBoolean isAnimationRemoved = new AtomicBoolean();

    private Consumer<UUID> stateCreated;
    private Consumer<UUID> stateRemoved;
    private Runnable onAnimationRemove;

    public static @NotNull AnimationEventHandler start() {
        return new AnimationEventHandler();
    }

    @ApiStatus.Internal
    public void stateCreated(@NotNull UUID uuid) {
        if (stateCreated == null) return;
        stateCreated.accept(uuid);
    }

    @ApiStatus.Internal
    public void stateRemoved(@NotNull UUID uuid) {
        if (stateRemoved == null) return;
        stateRemoved.accept(uuid);
    }

    @ApiStatus.Internal
    public void animationRemove() {
        if (onAnimationRemove == null) return;
        if (isAnimationRemoved.compareAndSet(false, true)) onAnimationRemove.run();
    }

    public @NotNull AnimationEventHandler onAnimationRemove(@NotNull Runnable runnable) {
        if (onAnimationRemove == null) onAnimationRemove = runnable;
        else {
            var previous = onAnimationRemove;
            onAnimationRemove = () -> {
                previous.run();
                runnable.run();
            };
        }
        return this;
    }

    public @NotNull AnimationEventHandler onStateCreated(@NotNull Consumer<UUID> consumer) {
        if (stateCreated == null) stateCreated = consumer;
        else stateCreated = stateCreated.andThen(consumer);
        return this;
    }

    public @NotNull AnimationEventHandler onStateRemoved(@NotNull Consumer<UUID> consumer) {
        if (stateRemoved == null) stateRemoved = consumer;
        else stateRemoved = stateRemoved.andThen(consumer);
        return this;
    }
}
