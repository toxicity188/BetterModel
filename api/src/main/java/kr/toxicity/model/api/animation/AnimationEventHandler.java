package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Animation event handler
 */
public final class AnimationEventHandler {

    private final AtomicBoolean isStateCreated = new AtomicBoolean();
    private final AtomicBoolean isStateRemoved = new AtomicBoolean();
    private final AtomicBoolean isAnimationRemoved = new AtomicBoolean();

    private Consumer<UUID> stateCreated;
    private Consumer<UUID> stateRemoved;
    private Runnable onAnimationRemove;

    /**
     * Starts event handler
     * @return new event handler
     */
    public static @NotNull AnimationEventHandler start() {
        return new AnimationEventHandler();
    }

    /**
     * Calls state created event
     * @param uuid target uuid
     */
    @ApiStatus.Internal
    public void stateCreated(@NotNull UUID uuid) {
        if (stateCreated == null) return;
        if (isStateCreated.compareAndSet(false, true)) stateCreated.accept(uuid);
    }

    /**
     * Calls state removed event
     * @param uuid target uuid
     */
    @ApiStatus.Internal
    public void stateRemoved(@NotNull UUID uuid) {
        if (stateRemoved == null) return;
        if (isStateRemoved.compareAndSet(false, true)) stateRemoved.accept(uuid);
    }


    /**
     * Calls animation remove event
     */
    @ApiStatus.Internal
    public void animationRemove() {
        if (onAnimationRemove == null) return;
        if (isAnimationRemoved.compareAndSet(false, true)) onAnimationRemove.run();
    }

    /**
     * Adds animation remove event handler
     * @param runnable event handler
     * @return self
     */
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

    /**
     * Adds state created event handler
     * @param consumer event handler
     * @return self
     */
    public @NotNull AnimationEventHandler onStateCreated(@NotNull Consumer<UUID> consumer) {
        if (stateCreated == null) stateCreated = consumer;
        else stateCreated = stateCreated.andThen(consumer);
        return this;
    }

    /**
     * Adds state removed event handler
     * @param consumer event handler
     * @return self
     */
    public @NotNull AnimationEventHandler onStateRemoved(@NotNull Consumer<UUID> consumer) {
        if (stateRemoved == null) stateRemoved = consumer;
        else stateRemoved = stateRemoved.andThen(consumer);
        return this;
    }
}
