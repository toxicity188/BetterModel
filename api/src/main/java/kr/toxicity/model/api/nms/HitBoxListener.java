package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.event.ModelDamageSource;
import org.jetbrains.annotations.NotNull;

public interface HitBoxListener {

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

    void sync(@NotNull HitBox hitBox);
    boolean damage(@NotNull ModelDamageSource source, double damage);
    void remove(@NotNull HitBox hitBox);
}
