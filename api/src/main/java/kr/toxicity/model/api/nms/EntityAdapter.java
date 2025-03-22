package kr.toxicity.model.api.nms;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public interface EntityAdapter {
    EntityAdapter EMPTY = new EntityAdapter() {

        @Override
        public boolean dead() {
            return false;
        }

        @Nullable
        @Override
        public LivingEntity entity() {
            return null;
        }

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
        public float damageTick() {
            return 0;
        }

        @Override
        public float walkSpeed() {
            return 1;
        }

        @Override
        public boolean fly() {
            return false;
        }

        @Override
        public @NotNull Vector3f passengerPosition() {
            return new Vector3f();
        }
    };

    default EntityAdapter multiply(
            double scaleMultiplier
    ) {
        return new Multiplier(
                this,
                scaleMultiplier
        );
    }

    record Multiplier(
            @NotNull EntityAdapter delegate,
            double scaleMultiplier
    ) implements EntityAdapter {

        @Override
        public boolean dead() {
            return delegate.dead();
        }

        @Nullable
        @Override
        public LivingEntity entity() {
            return delegate.entity();
        }

        @Override
        public boolean invisible() {
            return delegate.invisible();
        }

        @Override
        public boolean glow() {
            return delegate.glow();
        }

        @Override
        public boolean onWalk() {
            return delegate.onWalk();
        }

        @Override
        public float pitch() {
            return delegate.pitch();
        }

        @Override
        public float bodyYaw() {
            return delegate.bodyYaw();
        }

        @Override
        public float yaw() {
            return delegate.yaw();
        }

        @Override
        public double scale() {
            return delegate.scale() * scaleMultiplier;
        }

        @Override
        public float damageTick() {
            return delegate.damageTick();
        }

        @Override
        public float walkSpeed() {
            return delegate.walkSpeed();
        }

        @Override
        public boolean fly() {
            return delegate.fly();
        }

        @NotNull
        @Override
        public Vector3f passengerPosition() {
            return delegate.passengerPosition().div((float) scaleMultiplier);
        }
    }

    @Nullable LivingEntity entity();
    boolean dead();
    boolean invisible();
    boolean glow();
    boolean onWalk();
    boolean fly();
    double scale();
    float pitch();
    float bodyYaw();
    float yaw();
    float damageTick();
    float walkSpeed();
    @NotNull Vector3f passengerPosition();
}
