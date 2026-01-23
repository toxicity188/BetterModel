/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.event;

import kr.toxicity.model.api.event.ModelEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A wrapper class that adapts {@link ModelEvent} to Bukkit's {@link Event} system.
 * <p>
 * This allows Bukkit plugins to listen for BetterModel events using the standard Bukkit event API.
 * The underlying {@link ModelEvent} is lazily initialized when accessed.
 * </p>
 *
 * @since 2.0.0
 */
public final class BetterModelBukkitEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Class<? extends ModelEvent> eventClass;
    private final @NotNull Supplier<? extends ModelEvent> supplier;
    private volatile ModelEvent source;

    /**
     * Creates a new BetterModelBukkitEvent.
     *
     * @param eventClass the class of the model event
     * @param supplier a supplier that creates the model event
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public BetterModelBukkitEvent(@NotNull Class<? extends ModelEvent> eventClass, @NotNull Supplier<? extends ModelEvent> supplier) {
        super(!Bukkit.isPrimaryThread());
        this.eventClass = eventClass;
        this.supplier = supplier;
    }

    /**
     * Checks if the wrapped event is an instance of the specified class.
     *
     * @param eventClass the class to check against
     * @param <T> the type of the event
     * @return true if the wrapped event is assignable to the class
     * @since 2.0.0
     */
    public <T extends ModelEvent> boolean is(@NotNull Class<T> eventClass) {
        return eventClass.isAssignableFrom(this.eventClass);
    }

    /**
     * Casts the wrapped event to the specified class if possible.
     * <p>
     * This method initializes the underlying event if it hasn't been created yet.
     * </p>
     *
     * @param eventClass the class to cast to
     * @param <T> the type of the event
     * @return the cast event, or null if the cast is not possible
     * @since 2.0.0
     */
    public <T extends ModelEvent> @Nullable T as(@NotNull Class<T> eventClass) {
        if (!is(eventClass)) return null;
        var event = source;
        if (event == null) {
            synchronized (this) {
                event = source;
                if (event == null) event = source = supplier.get();
            }
        }
        return eventClass.cast(event);
    }

    /**
     * Executes a consumer if the wrapped event is of the specified type.
     *
     * @param eventClass the class to check against
     * @param consumer the consumer to execute
     * @param <T> the type of the event
     * @since 2.0.0
     */
    public <T extends ModelEvent> void as(@NotNull Class<T> eventClass, @NotNull Consumer<? super T> consumer) {
        var get = as(eventClass);
        if (get != null) consumer.accept(get);
    }

    /**
     * Returns the underlying model event, if initialized.
     *
     * @return the model event, or null if not yet initialized
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public @Nullable ModelEvent source() {
        return source;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Returns the handler list for this event.
     *
     * @return the handler list
     * @since 2.0.0
     */
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
