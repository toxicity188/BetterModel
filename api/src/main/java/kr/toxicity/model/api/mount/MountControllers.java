package kr.toxicity.model.api.mount;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public enum MountControllers implements MountController {
    INVALID {
        @NotNull
        @Override
        public Vector3f move(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
            return new Vector3f();
        }

        @Override
        public boolean canMount() {
            return false;
        }
    },
    NONE {
        @NotNull
        @Override
        public Vector3f move(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
            return new Vector3f();
        }

        @Override
        public boolean canControl() {
            return false;
        }
    },
    WALK {
        @NotNull
        @Override
        public Vector3f move(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
            input.y = 0;
            input.x = input.x * 0.5F;
            if (input.z <= 0.0F) {
                input.z *= 0.25F;
            }
            return input;
        }
    },
    FLY {
        @NotNull
        @Override
        public Vector3f move(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
            input.x = input.x * 0.5F;
            if (input.z <= 0.0F) {
                input.z *= 0.25F;
            }
            return input;
        }

        @Override
        public boolean canFly() {
            return true;
        }
    }
}
