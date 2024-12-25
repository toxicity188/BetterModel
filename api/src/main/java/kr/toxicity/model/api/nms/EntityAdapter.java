package kr.toxicity.model.api.nms;

public interface EntityAdapter {
    EntityAdapter EMPTY = new EntityAdapter() {
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
        public float yaw() {
            return 0;
        }
    };

    boolean onWalk();
    double scale();
    float bodyYaw();
    float yaw();
}
