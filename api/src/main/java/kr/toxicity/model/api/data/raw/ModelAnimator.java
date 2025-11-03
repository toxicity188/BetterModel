/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
 * A raw model animator.
 * @param name group name
 * @param keyframes keyframes
 * @param _rotationGlobal rotation global
 */
@ApiStatus.Internal
public record ModelAnimator(
        @Nullable String name,
        @Nullable List<ModelKeyframe> keyframes,
        @Nullable @SerializedName("rotation_global") Boolean _rotationGlobal
) {

    /**
     * Gets rotation global
     * @return rotation global
     */
    public boolean rotationGlobal() {
        return Boolean.TRUE.equals(_rotationGlobal);
    }

    /**
     * Checks this animator is available
     * @return is available
     */
    public boolean isAvailable() {
        return name != null && isNotEmpty();
    }

    /**
     * Checks this animator is not empty
     * @return is not empty
     */
    public boolean isNotEmpty() {
        return !keyframes().isEmpty();
    }

    /**
     * Gets name
     * @return name
     */
    @Override
    public @NotNull String name() {
        return Objects.requireNonNull(name);
    }

    /**
     * Gets keyframes
     * @return keyframes
     */
    @Override
    public @NotNull List<ModelKeyframe> keyframes() {
        return keyframes != null ? keyframes : Collections.emptyList();
    }

    /**
     * Gets keyframe stream
     * @return stream
     */
    public @NotNull Stream<ModelKeyframe> stream() {
        return keyframes().stream()
                .filter(ModelKeyframe::hasPoint)
                .sorted();
    }
}
