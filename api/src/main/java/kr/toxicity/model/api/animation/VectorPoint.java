/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.function.FloatConstantFunction;
import kr.toxicity.model.api.util.function.FloatFunction;
import kr.toxicity.model.api.util.interpolator.VectorInterpolator;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Vector point
 * @param function function
 * @param time time
 * @param interpolator interpolator
 */
public record VectorPoint(@NotNull FloatFunction<Vector3f> function, float time, @NotNull VectorInterpolator interpolator) implements Timed {

    /**
     * Empty point
     */
    public static final VectorPoint EMPTY = new VectorPoint(
            FloatFunction.of(new Vector3f()),
            0F,
            VectorInterpolator.defaultInterpolator()
    );

    /**
     * Gets vector by time
     * @param time time
     * @return vector
     */
    public @NotNull Vector3f vector(float time) {
        return function.apply(time);
    }

    /**
     * Checks this vector point is continuous
     * @return is continuous
     */
    public boolean isContinuous() {
        return function instanceof FloatConstantFunction && interpolator.isContinuous();
    }

    /**
     * Gets vector
     * @return vector
     */
    public @NotNull Vector3f vector() {
        return vector(time);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VectorPoint that)) return false;
        return Float.compare(time, that.time) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(time);
    }
}
