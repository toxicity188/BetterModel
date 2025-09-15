/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Animation event handler
 */
public final class AnimationEventHandler {

    private final AtomicBoolean isAnimationRemoved = new AtomicBoolean();
    private Runnable onAnimationRemove;

    /**
     * Starts event handler
     * @return new event handler
     */
    public static @NotNull AnimationEventHandler start() {
        return new AnimationEventHandler();
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
}
