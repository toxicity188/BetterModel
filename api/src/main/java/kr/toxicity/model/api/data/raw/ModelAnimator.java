/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A raw model animator.
 * @param name group name
 * @param keyframes keyframes
 */
@ApiStatus.Internal
public record ModelAnimator(
        @Nullable String name,
        @NotNull List<ModelKeyframe> keyframes
) {

    /**
     * Checks this animator has a name
     * @return has a name
     */
    public boolean hasName() {
        return name != null;
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
     * Gets keyframe stream
     * @return stream
     */
    public @NotNull Stream<ModelKeyframe> stream() {
        return keyframes.stream()
                .filter(ModelKeyframe::hasPoint)
                .sorted();
    }
}
