/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
 * A listener of hit-box
 */
public interface HitBoxListener {

    /**
     * Empty listener
     */
    HitBoxListener EMPTY = builder().build();

    /**
     * Creates builder of hitbox listener
     * @return listener builder
     */
    static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Creates builder by original hitbox listener
     * @return listener builder
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
     * Builder
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
         * Sets sync listener
         * @param sync listener
         * @return self
         */
        public @NotNull Builder sync(@NotNull Consumer<HitBox> sync) {
            this.sync = this.sync == DEFAULT_SYNC ? sync : this.sync.andThen(sync);
            return this;
        }

        /**
         * Sets damage listener
         * @param damage listener
         * @return self
         */
        public @NotNull Builder damage(@NotNull OnDamage damage) {
            this.damage = this.damage == DEFAULT_DAMAGE ? damage : this.damage.andThen(damage);
            return this;
        }

        /**
         * Sets remove listener
         * @param remove listener
         * @return self
         */
        public @NotNull Builder remove(@NotNull Consumer<HitBox> remove) {
            this.remove = this.remove == DEFAULT_REMOVE ? remove : this.remove.andThen(remove);
            return this;
        }

        /**
         * Sets mount listener
         * @param mount listener
         * @return self
         */
        public @NotNull Builder mount(@NotNull BiConsumer<HitBox, Entity> mount) {
            this.mount = this.mount == DEFAULT_MOUNT ? mount : this.mount.andThen(mount);
            return this;
        }

        /**
         * Sets dismount listener
         * @param dismount listener
         * @return self
         */
        public @NotNull Builder dismount(@NotNull BiConsumer<HitBox, Entity> dismount) {
            this.dismount = this.dismount == DEFAULT_DISMOUNT ? dismount : this.dismount.andThen(dismount);
            return this;
        }

        /**
         * Build it as listener
         * @return listener
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
     * On damage
     */
    interface OnDamage {
        /**
         * Handles damage event
         * @param hitBox hit-box
         * @param source damage source
         * @param damage damage amount
         * @return should be canceled
         */
        boolean event(@NotNull HitBox hitBox, @NotNull ModelDamageSource source, double damage);


        /**
         * Sums two damage handlers
         * @param other other
         * @return new handler
         */
        default @NotNull OnDamage andThen(@NotNull OnDamage other) {
            return (h, s, d) -> event(h, s, d) || other.event(h, s, d);
        }
    }

    /**
     * Listens to hit-box tick
     * @param hitBox target hit-box
     */
    void sync(@NotNull HitBox hitBox);

    /**
     * Listens to hit-box damage
     * @param hitBox target hit-box
     * @param source damage source
     * @param damage damage
     * @return cancel
     */
    boolean damage(@NotNull HitBox hitBox, @NotNull ModelDamageSource source, double damage);

    /**
     * Listens to hit-box remove
     * @param hitBox target hit-box
     */
    void remove(@NotNull HitBox hitBox);

    /**
     * Listens to hit-box mount
     * @param hitBox target hit-box
     * @param entity entity
     */
    void mount(@NotNull HitBox hitBox, @NotNull Entity entity);

    /**
     * Listens to hit-box dismount
     * @param hitBox target hit-box
     * @param entity entity
     */
    void dismount(@NotNull HitBox hitBox, @NotNull Entity entity);
}
