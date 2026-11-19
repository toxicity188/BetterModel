/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializer;
import kr.toxicity.model.api.animation.keyframe.KeyframeAxis;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.semver4j.Semver;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents metadata about the model file, specifically the format version.
 * <p>
 * This record is used to handle differences in coordinate systems and animation data between different Blockbench versions.
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
    public static final JsonDeserializer<ModelMeta> PARSER = (json, _, _) -> new ModelMeta(
        FormatVersion.find(Objects.requireNonNull(Semver.coerce(json.getAsJsonObject().getAsJsonPrimitive("format_version").getAsString())).getMajor())
    );

    /**
     * Enumerates supported Blockbench format versions and their specific coordinate conversions.
     * @since 1.15.2
     */
    @RequiredArgsConstructor
    public enum FormatVersion {
        /**
         * Blockbench version 5.0.0 and later.
         * @since 1.15.2
         */
        BLOCKBENCH_5(5) {

            private final KeyframeAxis pos = KeyframeAxis.of(
                KeyframeAxis.OPERATOR_INVERT.andThen(KeyframeAxis.OPERATOR_NORMALIZE_SCALE),
                KeyframeAxis.OPERATOR_NORMALIZE_SCALE,
                KeyframeAxis.OPERATOR_INVERT.andThen(KeyframeAxis.OPERATOR_NORMALIZE_SCALE)
            );

            private final KeyframeAxis rot = KeyframeAxis.of(
                KeyframeAxis.OPERATOR_INVERT,
                KeyframeAxis.OPERATOR_NONE,
                KeyframeAxis.OPERATOR_INVERT
            );

            @Override
            public @NotNull KeyframeAxis positionAxis() {
                return pos;
            }

            @Override
            public @NotNull KeyframeAxis rotationAxis() {
                return rot;
            }
        },
        /**
         * Legacy Blockbench versions (pre-5.0.0).
         * @since 1.15.2
         */
        BLOCKBENCH_LEGACY(0) {

            private final KeyframeAxis pos = KeyframeAxis.of(
                KeyframeAxis.OPERATOR_NORMALIZE_SCALE,
                KeyframeAxis.OPERATOR_NORMALIZE_SCALE,
                KeyframeAxis.OPERATOR_INVERT.andThen(KeyframeAxis.OPERATOR_NORMALIZE_SCALE)
            );

            private final KeyframeAxis rot = KeyframeAxis.of(
                KeyframeAxis.OPERATOR_NONE,
                KeyframeAxis.OPERATOR_INVERT,
                KeyframeAxis.OPERATOR_INVERT
            );

            @Override
            public @NotNull KeyframeAxis positionAxis() {
                return pos;
            }

            @Override
            public @NotNull KeyframeAxis rotationAxis() {
                return rot;
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


        public abstract @NotNull KeyframeAxis positionAxis();

        public abstract @NotNull KeyframeAxis rotationAxis();

        public @NotNull KeyframeAxis scaleAxis() {
            return KeyframeAxis.SCALE;
        }
    }
}
