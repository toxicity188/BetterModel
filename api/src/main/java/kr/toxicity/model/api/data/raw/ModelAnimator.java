/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Represents a raw animator from a model file, containing a set of keyframes for a specific bone group.
 *
 * @param name the name of the bone group this animator affects
 * @param keyframes the list of keyframes
 * @param _rotationGlobal whether the rotation is applied globally (true) or locally (false/null)
 * @since 1.15.2
 */
@ApiStatus.Internal
public record ModelAnimator(
    @Nullable String name,
    @Nullable List<ModelKeyframe> keyframes,
    @Nullable @SerializedName("rotation_global") Boolean _rotationGlobal
) {

    /**
     * Checks if the rotation should be applied globally.
     *
     * @return true if rotation is global, false otherwise
     * @since 1.15.2
     */
    public boolean rotationGlobal() {
        return Boolean.TRUE.equals(_rotationGlobal);
    }

    /**
     * Checks if this animator is valid and has keyframes.
     *
     * @return true if available, false otherwise
     * @since 1.15.2
     */
    public boolean isAvailable() {
        return name != null && isNotEmpty();
    }

    /**
     * Checks if this animator contains any keyframes.
     *
     * @return true if not empty, false otherwise
     * @since 1.15.2
     */
    public boolean isNotEmpty() {
        return !keyframes().isEmpty();
    }

    /**
     * Returns the name of the bone group this animator affects.
     *
     * @return the name of the bone group
     */
    @Override
    public @NotNull String name() {
        return Objects.requireNonNull(name);
    }

    /**
     * Returns the list of keyframes for this animator. If no keyframes are present, an empty list is returned.
     *
     * @return the list of keyframes
     */
    @Override
    public @NotNull List<ModelKeyframe> keyframes() {
        return keyframes != null ? keyframes : Collections.emptyList();
    }

    /**
     * Returns a sorted stream of valid keyframes.
     *
     * @return the stream of keyframes
     * @since 1.15.2
     */
    public @NotNull Stream<ModelKeyframe> stream() {
        return keyframes().stream()
            .filter(ModelKeyframe::hasPoint)
            .sorted();
    }
}
