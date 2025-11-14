/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializer;
import com.vdurmont.semver4j.Semver;
import kr.toxicity.model.api.util.MathUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Arrays;

/**
 * Model meta
 * @param formatVersion format version
 */
public record ModelMeta(
        @NotNull FormatVersion formatVersion
) {
    /**
     * Parser
     */
    public static final JsonDeserializer<ModelMeta> PARSER = (json, type, context) -> new ModelMeta(
            FormatVersion.find(new Semver(json.getAsJsonObject().getAsJsonPrimitive("format_version").getAsString(), Semver.SemverType.LOOSE).getMajor())
    );

    /**
     * Format version
     */
    @RequiredArgsConstructor
    public enum FormatVersion {
        /**
         * >=5.0.0
         */
        BLOCKBENCH_5(5) {
            @Override
            public @NotNull Vector3f convertAnimationRotation(@NotNull Vector3f vector) {
                vector.x = -vector.x;
                vector.z = -vector.z;
                return vector;
            }

            @Override
            public @NotNull Vector3f convertAnimationPosition(@NotNull Vector3f vector) {
                vector.x = -vector.x;
                vector.z = -vector.z;
                return vector.div(MathUtil.MODEL_TO_BLOCK_MULTIPLIER);
            }
        },
        /**
         * Legacy
         */
        BLOCKBENCH_LEGACY(0) {
            @Override
            public @NotNull Vector3f convertAnimationRotation(@NotNull Vector3f vector) {
                vector.y = -vector.y;
                vector.z = -vector.z;
                return vector;
            }

            @Override
            public @NotNull Vector3f convertAnimationPosition(@NotNull Vector3f vector) {
                vector.z = -vector.z;
                return vector.div(MathUtil.MODEL_TO_BLOCK_MULTIPLIER);
            }
        },
        ;

        private final int major;

        /**
         * Find matched version
         * @param major major version
         * @return version
         */
        public static @NotNull FormatVersion find(int major) {
            return Arrays.stream(values())
                    .filter(v -> v.major <= major)
                    .findFirst()
                    .orElseThrow();
        }

        /**
         * Converts animation rotation
         * @param vector target vector
         * @return converted vector
         */
        public abstract @NotNull Vector3f convertAnimationRotation(@NotNull Vector3f vector);

        /**
         * Converts animation position
         * @param vector target vector
         * @return converted vector
         */
        public abstract @NotNull Vector3f convertAnimationPosition(@NotNull Vector3f vector);

        /**
         * Converts animation scale
         * @param vector target vector
         * @return converted vector
         */
        public @NotNull Vector3f convertAnimationScale(@NotNull Vector3f vector) {
            vector.sub(1F, 1F, 1F);
            return vector;
        }
    }
}
