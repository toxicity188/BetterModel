package kr.toxicity.model.api.nms;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * An adapter of entity
 */
public interface EntityAdapter {
    /**
     * Empty adapter
     */
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

    /**
     * Multiplies this adapter
     * @param scaleMultiplier scale multiplier
     * @return multiplied adapter
     */
    default EntityAdapter multiply(
            double scaleMultiplier
    ) {
        return new Multiplier(
                this,
                scaleMultiplier
        );
    }

    /**
     * Multiplier
     * @param delegate source
     * @param scaleMultiplier scale multiplier
     */
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

    /**
     * Gets source
     * @return source entity
     */
    @Nullable LivingEntity entity();

    /**
     * Checks source entity is dead
     * @return dead
     */
    boolean dead();

    /**
     * Checks source entity is invisible
     * @return invisible
     */
    boolean invisible();

    /**
     * Checks source entity is on glow
     * @return glow
     */
    boolean glow();

    /**
     * Checks source entity is on walk
     * @return walk
     */
    boolean onWalk();

    /**
     * Checks source entity is on fly
     * @return fly
     */
    boolean fly();

    /**
     * Gets entity's scale
     * @return scale
     */
    double scale();

    /**
     * Gets entity's pitch (x-rot)
     * @return pitch
     */
    float pitch();

    /**
     * Gets entity's body yaw (y-rot)
     * @return body yaw
     */
    float bodyYaw();

    /**
     * Gets entity's yaw (y-rot)
     * @return yaw
     */
    float yaw();

    /**
     * Gets entity's damage tick
     * @return damage tick
     */
    float damageTick();

    /**
     * Gets entity's walk speed
     * @return walk speed
     */
    float walkSpeed();

    /**
     * Gets entity's passenger point
     * @return passenger point
     */
    @NotNull Vector3f passengerPosition();
}
