package kr.toxicity.model.api.nms;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface EntityAdapter {
    EntityAdapter EMPTY = new EntityAdapter() {
        @Override
        public boolean invisible() {
            return false;
        }

        @Override
        public boolean glow() {
            return false;
        }

        @Override
        public boolean onWalk() {
            return false;
        }

        @Override
        public double scale() {
            return 1;
        }

        @Override
        public float bodyYaw() {
            return 0;
        }

        @Override
        public float pitch() {
            return 0;
        }

        @Override
        public float yaw() {
            return 0;
        }

        @Override
        public @NotNull Vector3f passengerPosition() {
            return new Vector3f();
        }
    };

    boolean invisible();
    boolean glow();
    boolean onWalk();
    double scale();
    float pitch();
    float bodyYaw();
    float yaw();
    @NotNull Vector3f passengerPosition();
}
