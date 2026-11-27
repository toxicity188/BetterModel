/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api;

import kr.toxicity.model.api.event.ModelEvent;
import kr.toxicity.model.api.event.ModelEventApplication;
import kr.toxicity.model.api.event.ModelEventListener;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A central event bus for handling model-related events.
 * <p>
 * This interface allows subscribing to and publishing {@link ModelEvent}s.
 * It serves as a decoupling mechanism between different parts of the engine.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BetterModelEventBus eventBus = BetterModel.eventBus();
 * ModelEventListener listener = eventBus.subscribe(application, ModelEvent.class, event -> {
 *     // Handle event
 * });
 * }</pre>
 *
 * @since 2.0.0
 */
public interface BetterModelEventBus {

    /**
     * Subscribes a consumer to a specific event type.
     *
     * @param application the application that subscribes to the event
     * @param eventClass the exact class to subscribe to. Register each event class separately.
     * @param consumer the consumer to handle the event
     * @param <T> the type of the event
     * @return a handle for unregistering the subscription, or {@link ModelEventListener#NONE}
     * if the application is disabled
     * @since 2.0.0
     */
    @NotNull
    <T extends ModelEvent> ModelEventListener subscribe(@NotNull ModelEventApplication application, @NotNull Class<T> eventClass, @NotNull Consumer<T> consumer);

    /**
     * Publishes an event to all registered subscribers.
     * <p>
     * The supplier is evaluated when the dispatch path needs the event. A native platform
     * bridge may also request the event.
     * </p>
     *
     * @param eventClass the class of the event
     * @param eventSupplier a supplier that creates the event
     * @param <T> the type of the event
     * @return the result of the event call
     * @since 2.0.0
     */
    <T extends ModelEvent> @NotNull Result call(@NotNull Class<? extends T> eventClass, @NotNull Supplier<T> eventSupplier);

    /**
     * Publishes an event to all registered subscribers.
     *
     * @param event the event to publish
     * @return the result of the event call
     * @since 2.0.0
     */
    default @NotNull Result call(@NotNull ModelEvent event) {
        return call(event.getClass(), () -> event);
    }

    /**
     * Represents the outcome of an event publication.
     *
     * @since 2.0.0
     */
    @RequiredArgsConstructor
    enum Result {
        /**
         * Dispatch completed and the event was not cancelled.
         * @since 2.0.0
         */
        SUCCESS(true),
        /**
         * The event processing failed or was canceled.
         * @since 2.0.0
         */
        FAIL(false),
        /**
         * No handlers were registered for this event type.
         * @since 2.0.0
         */
        NO_EVENT_HANDLER(true)
        ;

        private final boolean triggered;

        /**
         * Checks if the event was considered "triggered" (i.e., not canceled or failed).
         * <p>
         * Note that {@link #NO_EVENT_HANDLER} is considered triggered as the operation wasn't blocked.
         * </p>
         *
         * @return true if triggered, false otherwise
         * @since 2.0.0
         */
        public boolean triggered() {
            return triggered;
        }
    }
}
