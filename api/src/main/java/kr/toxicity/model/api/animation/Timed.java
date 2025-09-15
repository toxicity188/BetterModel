/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;

/**
 * Object with keyframe time
 */
public interface Timed extends Comparable<Timed> {

    default int compareTo(@NotNull Timed o) {
        return Float.compare(time(), o.time());
    }

    /**
     * Gets time
     * @return time
     */
    float time();
}
