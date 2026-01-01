/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.event.ModelDamageSource;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

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
                .remove(this::remove)
                .mount(this::mount)
                .dismount(this::dismount);
    }

    /**
     * Builder for {@link HitBoxListener}.
     *
     * @since 1.15.2
     */
    class Builder {

        private static final Consumer<HitBox> DEFAULT_SYNC = h -> {};
        private static final OnDamage DEFAULT_DAMAGE = (h, s, d) -> false;
        private static final Consumer<HitBox> DEFAULT_REMOVE = h -> {};
        private static final BiConsumer<HitBox, Entity> DEFAULT_MOUNT = (h, e) -> {};
        private static final BiConsumer<HitBox, Entity> DEFAULT_DISMOUNT = (h, e) -> {};

        private Consumer<HitBox> sync = DEFAULT_SYNC;
        private OnDamage damage = DEFAULT_DAMAGE;
        private Consumer<HitBox> remove = DEFAULT_REMOVE;
        private BiConsumer<HitBox, Entity> mount = DEFAULT_MOUNT;
        private BiConsumer<HitBox, Entity> dismount = DEFAULT_DISMOUNT;

        /**
         * Private initializer
         */
        private Builder() {
        }

        /**
         * Adds a sync handler.
         *
         * @param sync the sync consumer
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder sync(@NotNull Consumer<HitBox> sync) {
            this.sync = this.sync == DEFAULT_SYNC ? sync : this.sync.andThen(sync);
            return this;
        }

        /**
         * Adds a damage handler.
         *
         * @param damage the damage handler
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder damage(@NotNull OnDamage damage) {
            this.damage = this.damage == DEFAULT_DAMAGE ? damage : this.damage.andThen(damage);
            return this;
        }

        /**
         * Adds a remove handler.
         *
         * @param remove the remove consumer
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder remove(@NotNull Consumer<HitBox> remove) {
            this.remove = this.remove == DEFAULT_REMOVE ? remove : this.remove.andThen(remove);
            return this;
        }

        /**
         * Adds a mount handler.
         *
         * @param mount the mount consumer
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder mount(@NotNull BiConsumer<HitBox, Entity> mount) {
            this.mount = this.mount == DEFAULT_MOUNT ? mount : this.mount.andThen(mount);
            return this;
        }

        /**
         * Adds a dismount handler.
         *
         * @param dismount the dismount consumer
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder dismount(@NotNull BiConsumer<HitBox, Entity> dismount) {
            this.dismount = this.dismount == DEFAULT_DISMOUNT ? dismount : this.dismount.andThen(dismount);
            return this;
        }

        /**
         * Builds the listener.
         *
         * @return the created listener
         * @since 1.15.2
         */
        public @NotNull HitBoxListener build() {
            return new HitBoxListener() {
                @Override
                public void sync(@NotNull HitBox hitBox) {
                    sync.accept(hitBox);
                }

                @Override
                public boolean damage(@NotNull HitBox hitBox, @NotNull ModelDamageSource source, double damage) {
                    return Builder.this.damage.event(hitBox, source, damage);
                }

                @Override
                public void remove(@NotNull HitBox hitBox) {
                    remove.accept(hitBox);
                }

                @Override
                public void mount(@NotNull HitBox hitBox, @NotNull Entity entity) {
                    mount.accept(hitBox, entity);
                }

                @Override
                public void dismount(@NotNull HitBox hitBox, @NotNull Entity entity) {
                    dismount.accept(hitBox, entity);
                }
            };
        }
    }

    /**
     * Functional interface for handling damage events.
     *
     * @since 1.15.2
     */
    interface OnDamage {
        /**
         * Handles a damage event.
         *
         * @param hitBox the target hitbox
         * @param source the damage source
         * @param damage the damage amount
         * @return true to cancel the damage, false otherwise
         * @since 1.15.2
         */
        boolean event(@NotNull HitBox hitBox, @NotNull ModelDamageSource source, double damage);


        /**
         * Chains this handler with another.
         *
         * @param other the other handler
         * @return the combined handler
         * @since 1.15.2
         */
        default @NotNull OnDamage andThen(@NotNull OnDamage other) {
            return (h, s, d) -> event(h, s, d) || other.event(h, s, d);
        }
    }

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
     * @param hitBox the target hitbox
     * @param source the damage source
     * @param damage the damage amount
     * @return true if the damage was cancelled
     * @since 1.15.2
     */
    boolean damage(@NotNull HitBox hitBox, @NotNull ModelDamageSource source, double damage);

    /**
     * Called when the hitbox is removed.
     *
     * @param hitBox the target hitbox
     * @since 1.15.2
     */
    void remove(@NotNull HitBox hitBox);

    /**
     * Called when an entity mounts the hitbox.
     *
     * @param hitBox the target hitbox
     * @param entity the mounting entity
     * @since 1.15.2
     */
    void mount(@NotNull HitBox hitBox, @NotNull Entity entity);

    /**
     * Called when an entity dismounts the hitbox.
     *
     * @param hitBox the target hitbox
     * @param entity the dismounting entity
     * @since 1.15.2
     */
    void dismount(@NotNull HitBox hitBox, @NotNull Entity entity);
}
