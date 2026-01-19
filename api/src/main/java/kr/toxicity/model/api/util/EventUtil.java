/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelEventBus;
import kr.toxicity.model.api.event.CancellableEvent;
import kr.toxicity.model.api.event.ModelEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Utility class for dispatching model events.
 * <p>
 * This class provides helper methods to call events on the {@link BetterModelEventBus}
 * and handle cancellable events conveniently.
 * </p>
 *
 * @since 2.0.0
 */
@ApiStatus.Internal
public final class EventUtil {
    /**
     * Private initializer to prevent instantiation.
     */
    private EventUtil() {
        throw new RuntimeException();
    }

    /**
     * Calls an event on the global event bus.
     *
     * @param eventClass the class of the event
     * @param eventSupplier a supplier that creates the event
     * @param <T> the type of the event
     * @return the result of the event call
     * @since 2.0.0
     */
    @NotNull
    public static <T extends ModelEvent> BetterModelEventBus.Result call(@NotNull Class<T> eventClass, @NotNull Supplier<T> eventSupplier) {
        return BetterModel.eventBus().call(eventClass, eventSupplier);
    }

    /**
     * Calls an event and checks if it was cancelled.
     * <p>
     * If the event is cancellable, this method returns true only if the event was NOT cancelled.
     * If the event is not cancellable, it always returns true.
     * </p>
     *
     * @param event the event to call
     * @return true if the event should proceed (not cancelled), false otherwise
     * @since 2.0.0
     */
    public static boolean call(@NotNull ModelEvent event) {
        event.call();
        return !(event instanceof CancellableEvent cancellableEvent) || !cancellableEvent.isCancelled();
    }
}
