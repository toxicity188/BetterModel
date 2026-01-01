/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.mount;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Builtin mount controllers
 */
public enum MountControllers implements MountController {
    /**
     * Invalid
     */
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
    /**
     * None
     */
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
    /**
     * Walk
     */
    WALK {
        @NotNull
        @Override
        public Vector3f move(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
            input.normalize();
            input.y = 0;
            input.x = input.x * 0.5F;
            if (input.z <= 0.0F) {
                input.z *= 0.25F;
            }
            return input;
        }
    },
    /**
     * Fly
     */
    FLY {
        @NotNull
        @Override
        public Vector3f move(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector) {
            input.normalize();
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
