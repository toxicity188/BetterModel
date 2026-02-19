/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.event.DismountModelEvent;
import kr.toxicity.model.api.event.HitBoxEvent;
import kr.toxicity.model.api.event.HitBoxRemoveEvent;
import kr.toxicity.model.api.event.HitBoxSyncEvent;
import kr.toxicity.model.api.event.ModelDamagedEvent;
import kr.toxicity.model.api.event.ModelInteractAtEvent;
import kr.toxicity.model.api.event.ModelInteractEvent;
import kr.toxicity.model.api.event.MountModelEvent;
import kr.toxicity.model.api.platform.PlatformEntity;
import org.jetbrains.annotations.NotNull;

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
        return new Builder();
    }

    /**
     * Creates a builder initialized with this listener's current handlers.
     *
     * @return a new builder
     * @since 1.15.2
     */
    default @NotNull Builder toBuilder() {
        return new Builder()
                .sync(this::sync)
                .damage(this::damage)
                .interact(this::interact)
                .interactAt(this::interactAt)
                .remove(this::remove)
                .mount(this::mount)
                .dismount(this::dismount);
    }

    /**
     * Builder for {@link HitBoxListener}.
     *
     * @since 1.15.2
     */
    final class Builder {

        private final Map<Class<? extends HitBoxEvent>, Consumer<?>> listeners = new HashMap<>();

        /**
         * Private initializer.
         */
        private Builder() {
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
            var old = listeners.get(eventClass);
            listeners.put(eventClass, old == null ? consumer : ((Consumer<T>) old).andThen(consumer));
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
            return listen(HitBoxSyncEvent.class, event -> sync.accept(event.getHitBox()));
        }

        /**
         * Adds a damage handler.
         *
         * @param damage the damage handler
         * @return this builder
         * @since 2.0.2
         */
        public @NotNull Builder damage(@NotNull Consumer<ModelDamagedEvent> damage) {
            return listen(ModelDamagedEvent.class, damage);
        }

        /**
         * Adds an interact handler.
         *
         * @param interact the interact handler
         * @return this builder
         * @since 2.0.2
         */
        public @NotNull Builder interact(@NotNull Consumer<ModelInteractEvent> interact) {
            return listen(ModelInteractEvent.class, interact);
        }

        /**
         * Adds an interact-at handler.
         *
         * @param interactAt the interact-at handler
         * @return this builder
         * @since 2.0.2
         */
        public @NotNull Builder interactAt(@NotNull Consumer<ModelInteractAtEvent> interactAt) {
            return listen(ModelInteractAtEvent.class, interactAt);
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
            return listen(MountModelEvent.class, event -> mount.accept(event.getHitBox(), event.entity()));
        }

        /**
         * Adds a dismount handler.
         *
         * @param dismount the dismount consumer
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder dismount(@NotNull BiConsumer<HitBox, PlatformEntity> dismount) {
            return listen(DismountModelEvent.class, event -> dismount.accept(event.getHitBox(), event.entity()));
        }

        /**
         * Builds the listener.
         *
         * @return the created listener
         * @since 1.15.2
         */
        public @NotNull HitBoxListener build() {
            var copied = new HashMap<>(listeners);
            return new HitBoxListener() {
                @Override
                @SuppressWarnings("unchecked")
                public void handle(@NotNull HitBoxEvent event) {
                    var consumer = (Consumer<HitBoxEvent>) copied.get(event.getClass());
                    if (consumer != null) {
                        consumer.accept(event);
                    }
                }

                @Override
                public void sync(@NotNull HitBox hitBox) {
                    handle(new HitBoxSyncEvent(hitBox));
                }

                @Override
                public void damage(@NotNull ModelDamagedEvent event) {
                    handle(event);
                }

                @Override
                public void interact(@NotNull ModelInteractEvent event) {
                    handle(event);
                }

                @Override
                public void interactAt(@NotNull ModelInteractAtEvent event) {
                    handle(event);
                }

                @Override
                public void remove(@NotNull HitBox hitBox) {
                    handle(new HitBoxRemoveEvent(hitBox));
                }

                @Override
                public void mount(@NotNull HitBox hitBox, @NotNull PlatformEntity PlatformEntity) {
                    var bone = hitBox.positionSource();
                    handle(new MountModelEvent(bone.tracker(), bone, hitBox, PlatformEntity));
                }

                @Override
                public void dismount(@NotNull HitBox hitBox, @NotNull PlatformEntity PlatformEntity) {
                    var bone = hitBox.positionSource();
                    handle(new DismountModelEvent(bone.tracker(), bone, hitBox, PlatformEntity));
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
     * Called when the hitbox is synchronized (ticked).
     *
     * @param hitBox the target hitbox
     * @since 1.15.2
     */
    void sync(@NotNull HitBox hitBox);

    /**
     * Called when the hitbox receives damage.
     *
     * @param event the damage event
     * @since 2.0.2
     */
    void damage(@NotNull ModelDamagedEvent event);

    /**
     * Called when the hitbox receives an interaction.
     *
     * @param event the interaction event
     * @since 2.0.2
     */
    void interact(@NotNull ModelInteractEvent event);

    /**
     * Called when the hitbox receives an interaction at a specific position.
     *
     * @param event the interaction-at event
     * @since 2.0.2
     */
    void interactAt(@NotNull ModelInteractAtEvent event);

    /**
     * Called when the hitbox is removed.
     *
     * @param hitBox the target hitbox
     * @since 1.15.2
     */
    void remove(@NotNull HitBox hitBox);

    /**
     * Called when an PlatformEntity mounts the hitbox.
     *
     * @param hitBox the target hitbox
     * @param PlatformEntity the mounting PlatformEntity
     * @since 1.15.2
     */
    void mount(@NotNull HitBox hitBox, @NotNull PlatformEntity PlatformEntity);

    /**
     * Called when an PlatformEntity dismounts the hitbox.
     *
     * @param hitBox the target hitbox
     * @param PlatformEntity the dismounting PlatformEntity
     * @since 1.15.2
     */
    void dismount(@NotNull HitBox hitBox, @NotNull PlatformEntity PlatformEntity);
}
