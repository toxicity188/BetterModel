package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.event.ModelDamageSource;
import org.jetbrains.annotations.NotNull;

/**
 * A listener of hit-box
 */
public interface HitBoxListener {

    /**
     * Empty listener
     */
    HitBoxListener EMPTY = new HitBoxListener() {
        @Override
        public void sync(@NotNull HitBox hitBox) {

        }

        @Override
        public boolean damage(@NotNull ModelDamageSource source, double damage) {
            return false;
        }

        @Override
        public void remove(@NotNull HitBox hitBox) {

        }
    };

    /**
     * Listens hit-box tick
     * @param hitBox target hit-box
     */
    void sync(@NotNull HitBox hitBox);

    /**
     * Listens hit-box damage
     * @param source damage source
     * @param damage damage
     * @return cancel
     */
    boolean damage(@NotNull ModelDamageSource source, double damage);

    /**
     * Listens hit-box remove
     * @param hitBox target hit-box
     */
    void remove(@NotNull HitBox hitBox);
}
