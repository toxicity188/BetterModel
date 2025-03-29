package kr.toxicity.model.api.mount;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface MountController {
    @NotNull Vector3f move(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector);
    default @NotNull Vector3f moveOnFly(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
        return move(player, entity, input, travelVector).mul(1.5F);
    }
    default Vector3f move(@NotNull MoveType type, @NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
        return switch (type) {
            case DEFAULT -> move(player, entity, input, travelVector);
            case FLY -> moveOnFly(player, entity, input, travelVector);
        };
    }
    default boolean canMount() {
        return true;
    }
    default boolean canDismountBySelf() {
        return true;
    }
    default boolean canControl() {
        return true;
    }
    default boolean canJump() {
        return true;
    }
    default boolean canFly() {
        return false;
    }

    enum MoveType {
        DEFAULT,
        FLY
    }

    default @NotNull Modifier modifier() {
        return new Modifier(this);
    }

    class Modifier {

        private final MountController source;

        private boolean canMount;
        private boolean canDismountBySelf;
        private boolean canControl;
        private boolean canJump;
        private boolean canFly;

        private Modifier(@NotNull MountController controller) {
            this.source = controller;
            canMount = controller.canMount();
            canDismountBySelf = controller.canDismountBySelf();
            canControl = controller.canControl();
            canJump = controller.canJump();
            canFly = controller.canFly();
        }

        public @NotNull Modifier canDismountBySelf(boolean canDismountBySelf) {
            this.canDismountBySelf = canDismountBySelf;
            return this;
        }

        public @NotNull Modifier canMount(boolean canMount) {
            this.canMount = canMount;
            return this;
        }

        public @NotNull Modifier canControl(boolean canControl) {
            this.canControl = canControl;
            return this;
        }

        public @NotNull Modifier canFly(boolean canFly) {
            this.canFly = canFly;
            return this;
        }

        public @NotNull Modifier canJump(boolean canJump) {
            this.canJump = canJump;
            return this;
        }

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
