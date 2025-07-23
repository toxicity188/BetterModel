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
     * Creates build of hit-box listener
     * @return listener builder
     */
    static @NotNull Builder builder() {
        return new Builder();
    }

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
        private Consumer<HitBox> sync = h -> {};
        private OnDamage damage = (h, s, d) -> false;
        private Consumer<HitBox> remove = h -> {};
        private BiConsumer<HitBox, Entity> mount = (h, e) -> {};
        private BiConsumer<HitBox, Entity> dismount = (h, e) -> {};

        /**
         * Private initializer
         */
        private Builder() {
        }

        /**
         * Sets damage listener
         * @param damage listener
         * @return self
         */
        public @NotNull Builder damage(@NotNull OnDamage damage) {
            this.damage = damage;
            return this;
        }

        /**
         * Sets dismount listener
         * @param dismount listener
         * @return self
         */
        public @NotNull Builder dismount(@NotNull BiConsumer<HitBox, Entity> dismount) {
            this.dismount = this.dismount.andThen(dismount);
            return this;
        }

        /**
         * Sets mount listener
         * @param mount listener
         * @return self
         */
        public @NotNull Builder mount(@NotNull BiConsumer<HitBox, Entity> mount) {
            this.mount = this.mount.andThen(mount);
            return this;
        }

        /**
         * Sets remove listener
         * @param remove listener
         * @return self
         */
        public @NotNull Builder remove(@NotNull Consumer<HitBox> remove) {
            this.remove = this.remove.andThen(remove);
            return this;
        }

        /**
         * Sets sync listener
         * @param sync listener
         * @return self
         */
        public @NotNull Builder sync(@NotNull Consumer<HitBox> sync) {
            this.sync = this.sync.andThen(sync);
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

    interface OnDamage {
        boolean event(@NotNull HitBox hitBox, @NotNull ModelDamageSource source, double damage);
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
