/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import com.google.common.collect.ImmutableMap;
import kr.toxicity.model.api.event.hitbox.*;
import kr.toxicity.model.api.platform.PlatformEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Listens for events related to a {@link HitBox}, such as damage, interaction, and mounting.
 * <p>
 * This interface allows for custom behavior when a hitbox is interacted with.
 * </p>
 *
 * @since 1.15.2
 */
public interface HitBoxListener {

    /**
     * An empty listener that does nothing.
     * @since 1.15.2
     */
    HitBoxListener EMPTY = builder().build();

    /**
     * Creates a new builder for {@link HitBoxListener}.
     *
     * @return a new builder
     * @since 1.15.2
     */
    static @NotNull Builder builder() {
        return new Builder(new HashMap<>(), null);
    }

    /**
     * Builder for {@link HitBoxListener}.
     *
     * @since 1.15.2
     */
    final class Builder {

        private final Map<Class<? extends HitBoxEvent>, Consumer<?>> listeners;
        private Consumer<HitBox> syncConsumer;

        /**
         * Private initializer.
         */
        private Builder(
            @NotNull Map<Class<? extends HitBoxEvent>, Consumer<?>> listeners,
            @Nullable Consumer<HitBox> syncConsumer
        ) {
            this.listeners = listeners;
            this.syncConsumer = syncConsumer;
        }

        /**
         * Adds a handler for the specified hitbox event class.
         *
         * @param eventClass event class
         * @param consumer event consumer
         * @param <T> event type
         * @return this builder
         * @since 2.1.0
         */
        @SuppressWarnings("unchecked")
        public <T extends HitBoxEvent> @NotNull Builder listen(@NotNull Class<T> eventClass, @NotNull Consumer<T> consumer) {
            listeners.compute(eventClass, (k, old) -> old == null ? consumer : ((Consumer<T>) old).andThen(consumer));
            return this;
        }

        /**
         * Adds a sync handler.
         *
         * @param sync the sync consumer
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder sync(@NotNull Consumer<HitBox> sync) {
            var previous = syncConsumer;
            syncConsumer = previous != null ? previous.andThen(sync) : sync;
            return this;
        }

        /**
         * Adds a damage handler.
         *
         * @param damage the damage handler
         * @return this builder
         * @since 2.0.2
         */
        public @NotNull Builder damage(@NotNull Consumer<HitBoxDamagedEvent> damage) {
            return listen(HitBoxDamagedEvent.class, damage);
        }

        /**
         * Adds an interact handler.
         *
         * @param interact the interact handler
         * @return this builder
         * @since 2.0.2
         */
        public @NotNull Builder interact(@NotNull Consumer<HitBoxInteractEvent> interact) {
            return listen(HitBoxInteractEvent.class, interact);
        }

        /**
         * Adds an interact-at handler.
         *
         * @param interactAt the interact-at handler
         * @return this builder
         * @since 2.0.2
         */
        public @NotNull Builder interactAt(@NotNull Consumer<HitBoxInteractAtEvent> interactAt) {
            return listen(HitBoxInteractAtEvent.class, interactAt);
        }

        /**
         * Adds a remove handler.
         *
         * @param remove the remove consumer
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder remove(@NotNull Consumer<HitBox> remove) {
            return listen(HitBoxRemoveEvent.class, event -> remove.accept(event.getHitBox()));
        }

        /**
         * Adds a mount handler.
         *
         * @param mount the mount consumer
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder mount(@NotNull BiConsumer<HitBox, PlatformEntity> mount) {
            return listen(HitBoxMountEvent.class, event -> mount.accept(event.getHitBox(), event.entity()));
        }

        /**
         * Adds a dismount handler.
         *
         * @param dismount the dismount consumer
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder dismount(@NotNull BiConsumer<HitBox, PlatformEntity> dismount) {
            return listen(HitBoxDismountEvent.class, event -> dismount.accept(event.getHitBox(), event.entity()));
        }

        /**
         * Builds the listener.
         *
         * @return the created listener
         * @since 1.15.2
         */
        @SuppressWarnings("unchecked")
        public @NotNull HitBoxListener build() {
            var copied = ImmutableMap.copyOf(listeners);
            var sync = syncConsumer;
            return new HitBoxListener() {
                @Override
                @SuppressWarnings("unchecked")
                public void handle(@NotNull HitBoxEvent event) {
                    var consumer = (Consumer<HitBoxEvent>) copied.get(event.getClass());
                    if (consumer != null) consumer.accept(event);
                }

                @Override
                public void sync(@NotNull HitBox hitBox) {
                    if (sync != null) sync.accept(hitBox);
                }

                @Override
                public @NotNull Builder toBuilder() {
                    return new Builder(new HashMap<>(copied), sync);
                }
            };
        }
    }

    /**
     * Handles a hitbox event.
     *
     * @param event target event
     * @since 2.1.0
     */
    void handle(@NotNull HitBoxEvent event);

    /**
     * Handles tick method.
     *
     * @param hitBox target hitbox
     * @since 2.1.0
     */
    void sync(@NotNull HitBox hitBox);

    /**
     * Creates a builder initialized with this listener's current handlers.
     *
     * @return a new builder
     * @since 1.15.2
     */
    @NotNull Builder toBuilder();
}
