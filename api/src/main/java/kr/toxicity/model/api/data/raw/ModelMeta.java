/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
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
 * Represents metadata about the model file, specifically the format version.
 * <p>
 * This record is used to handle differences in coordinate systems and animation data between different BlockBench versions.
 * </p>
 *
 * @param formatVersion the detected format version of the model file
 * @since 1.15.2
 */
public record ModelMeta(
    @NotNull FormatVersion formatVersion
) {
    /**
     * A JSON deserializer for parsing {@link ModelMeta} from the "meta" object in a .bbmodel file.
     * @since 1.15.2
     */
    public static final JsonDeserializer<ModelMeta> PARSER = (json, type, context) -> new ModelMeta(
        FormatVersion.find(new Semver(json.getAsJsonObject().getAsJsonPrimitive("format_version").getAsString(), Semver.SemverType.LOOSE).getMajor())
    );

    /**
     * Enumerates supported BlockBench format versions and their specific coordinate conversions.
     * @since 1.15.2
     */
    @RequiredArgsConstructor
    public enum FormatVersion {
        /**
         * BlockBench version 5.0.0 and later.
         * @since 1.15.2
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
         * Legacy BlockBench versions (pre-5.0.0).
         * @since 1.15.2
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
         * Finds the appropriate format version based on the major version number.
         *
         * @param major the major version number
         * @return the matching format version
         * @throws java.util.NoSuchElementException if no matching version is found
         * @since 1.15.2
         */
        public static @NotNull FormatVersion find(int major) {
            return Arrays.stream(values())
                .filter(v -> v.major <= major)
                .findFirst()
                .orElseThrow();
        }

        /**
         * Converts animation rotation values to the engine's coordinate system.
         *
         * @param vector the raw rotation vector
         * @return the converted rotation vector
         * @since 1.15.2
         */
        public abstract @NotNull Vector3f convertAnimationRotation(@NotNull Vector3f vector);

        /**
         * Converts animation position values to the engine's coordinate system.
         *
         * @param vector the raw position vector
         * @return the converted position vector
         * @since 1.15.2
         */
        public abstract @NotNull Vector3f convertAnimationPosition(@NotNull Vector3f vector);

        /**
         * Converts animation scale values to the engine's format (relative to 1.0).
         *
         * @param vector the raw scale vector
         * @return the converted scale vector
         * @since 1.15.2
         */
        public @NotNull Vector3f convertAnimationScale(@NotNull Vector3f vector) {
            vector.sub(1F, 1F, 1F);
            return vector;
        }
    }
}
