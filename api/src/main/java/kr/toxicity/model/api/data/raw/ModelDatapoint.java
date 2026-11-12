/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonPrimitive;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.keyframe.KeyframeDatapoint;
import kr.toxicity.model.api.animation.keyframe.KeyframeDatapointVector;
import kr.toxicity.model.api.util.function.Float2FloatConstantFunction;
import kr.toxicity.model.api.util.function.Float2FloatFunction;
import kr.toxicity.model.api.util.function.FloatFunction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Represents a single data point within a keyframe, which can be a static value or a Molang script.
 * <p>
 * This record holds the raw JSON values for x, y, and z coordinates, or a script string.
 * </p>
 *
 * @param x the x-coordinate value or script
 * @param y the y-coordinate value or script
 * @param z the z-coordinate value or script
 * @param script the script string (used for sound/particle effects)
 * @since 1.15.2
 */
@ApiStatus.Internal
public record ModelDatapoint(
    @Nullable JsonPrimitive x,
    @Nullable JsonPrimitive y,
    @Nullable JsonPrimitive z,
    @Nullable String script
) {

    /**
     * Checks if this data point contains a script.
     *
     * @return true if a script is present, false otherwise
     * @since 1.15.2
     */
    public boolean hasScript() {
        return script != null;
    }

    /**
     * Returns the script string.
     *
     * @return the script
     * @throws NullPointerException if no script is present
     * @since 1.15.2
     */
    @Override
    public @NotNull String script() {
        return Objects.requireNonNull(script);
    }

    public @NotNull KeyframeDatapointVector toVector(@NotNull ModelLoadContext context) {
        return KeyframeDatapointVector.of(
            KeyframeDatapoint.fromJson(x, context.placeholder::parseVariable),
            KeyframeDatapoint.fromJson(y, context.placeholder::parseVariable),
            KeyframeDatapoint.fromJson(z, context.placeholder::parseVariable)
        );
    }
}
