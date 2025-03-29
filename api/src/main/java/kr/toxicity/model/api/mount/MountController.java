package kr.toxicity.model.api.mount;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * A mount controller of hit-box
 */
public interface MountController {

    /**
     * Moves entity by player's input
     * @param player passenger
     * @param entity target
     * @param input input vector
     * @param travelVector travel vector
     * @return movement
     */
    @NotNull Vector3f move(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector);

    /**
     * Moves entity by player's input on fly
     * @param player passenger
     * @param entity target
     * @param input input vector
     * @param travelVector travel vector
     * @return movement
     */
    default @NotNull Vector3f moveOnFly(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
        return move(player, entity, input, travelVector).mul(1.5F);
    }

    /**
     * Moves entity by player's input by type
     * @param type type
     * @param player passenger
     * @param entity target
     * @param input input vector
     * @param travelVector travel vector
     * @return movement
     */
    default Vector3f move(@NotNull MoveType type, @NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
        return switch (type) {
            case DEFAULT -> move(player, entity, input, travelVector);
            case FLY -> moveOnFly(player, entity, input, travelVector);
        };
    }

    /**
     * Checks some player can mount
     * @return can mount
     */
    default boolean canMount() {
        return true;
    }

    /**
     * Checks some player can dismount by self (right click or sneak)
     * @return can dismount by self
     */
    default boolean canDismountBySelf() {
        return true;

    }

    /**
     * Checks some player can control
     * @return can control
     */
    default boolean canControl() {
        return true;
    }

    /**
     * Checks some player can jump
     * @return can jump
     */
    default boolean canJump() {
        return true;
    }

    /**
     * Checks some player can fly
     * @return can fly
     */
    default boolean canFly() {
        return false;
    }

    /**
     * Movement type
     */
    enum MoveType {
        /**
         * On ground
         */
        DEFAULT,
        /**
         * On fly
         */
        FLY
    }

    /**
     * Creates modifier of this controller
     * @return modifier
     */
    default @NotNull Modifier modifier() {
        return new Modifier(this);
    }

    /**
     * Modifier
     */
    class Modifier {

        private final MountController source;

        private boolean canMount;
        private boolean canDismountBySelf;
        private boolean canControl;
        private boolean canJump;
        private boolean canFly;

        /**
         * Modifier of source
         * @param controller source
         */
        private Modifier(@NotNull MountController controller) {
            this.source = controller;
            canMount = controller.canMount();
            canDismountBySelf = controller.canDismountBySelf();
            canControl = controller.canControl();
            canJump = controller.canJump();
            canFly = controller.canFly();
        }

        /**
         * Sets some player can dismount by self (right click or sneak)
         * @param canDismountBySelf can dismount
         * @return self
         */
        public @NotNull Modifier canDismountBySelf(boolean canDismountBySelf) {
            this.canDismountBySelf = canDismountBySelf;
            return this;
        }

        /**
         * Sets some player can mount
         * @param canMount can mount
         * @return self
         */
        public @NotNull Modifier canMount(boolean canMount) {
            this.canMount = canMount;
            return this;
        }

        /**
         * Sets some player can control
         * @param canControl can control
         * @return self
         */
        public @NotNull Modifier canControl(boolean canControl) {
            this.canControl = canControl;
            return this;
        }

        /**
         * Sets some player can fly
         * @param canFly can fly
         * @return self
         */
        public @NotNull Modifier canFly(boolean canFly) {
            this.canFly = canFly;
            return this;
        }

        /**
         * Sets some player can jump
         * @param canJump can jump
         * @return self
         */
        public @NotNull Modifier canJump(boolean canJump) {
            this.canJump = canJump;
            return this;
        }

        /**
         * Builds controller with modified value
         * @return modified controller
         */
        public @NotNull MountController build() {
            return new MountController() {
                @NotNull
                @Override
                public Vector3f move(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
                    return source.move(player, entity, input, travelVector);
                }

                @Override
                public boolean canDismountBySelf() {
                    return canDismountBySelf;
                }

                @Override
                public boolean canControl() {
                    return canControl;
                }

                @Override
                public boolean canFly() {
                    return canFly;
                }

                @Override
                public boolean canJump() {
                    return canJump;
                }

                @Override
                public boolean canMount() {
                    return canMount;
                }
            };
        }
    }
}
