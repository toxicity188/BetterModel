/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Object with keyframe time
 */
public interface Timed extends Comparable<Timed> {

    default int compareTo(@NotNull Timed o) {
        return MathUtil.FRAME_COMPARATOR.compare(time(), o.time());
    }

    /**
     * Gets time
     * @return time
     */
    float time();

    /**
     * Returns this time in seconds, rounded to the nearest 50 ms Minecraft tick.
     * <pre>{@code
     * float playbackTime = keyframe.tickTime();
     * }</pre>
     *
     * @return the rounded time in seconds
     * @since 3.7.2
     */
    default float tickTime() {
        return InterpolationUtil.roundTick(time());
    }
}
