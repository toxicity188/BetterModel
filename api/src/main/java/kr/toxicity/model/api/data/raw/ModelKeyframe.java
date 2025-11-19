/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.animation.Timed;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.util.interpolator.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A keyframe of model.
 * @param channel channel
 * @param dataPoints movement
 * @param bezierLeftTime bezier left time
 * @param bezierLeftValue bezier left value
 * @param bezierRightTime bezier right time
 * @param bezierRightValue bezier right value
 * @param interpolation interpolator type
 * @param time keyframe time
 */
@ApiStatus.Internal
public record ModelKeyframe(
        @Nullable KeyframeChannel channel,
        @SerializedName("data_points") @NotNull List<Datapoint> dataPoints,
        @SerializedName("bezier_left_time") @Nullable Float3 bezierLeftTime,
        @SerializedName("bezier_left_value") @Nullable Float3 bezierLeftValue,
        @SerializedName("bezier_right_time") @Nullable Float3 bezierRightTime,
        @SerializedName("bezier_right_value") @Nullable Float3 bezierRightValue,
        @Nullable VectorInterpolator interpolation,
        float time
) implements Timed {

    /**
     * Checks this keyframe has data point
     * @return has point
     */
    public boolean hasPoint() {
        return !dataPoints.isEmpty();
    }

    /**
     * Gets first data point
     * @return point
     */
    public @NotNull Datapoint point() {
        return dataPoints.getFirst();
    }

    /**
     * Gets vector point
     * @param context context
     * @param function function
     * @return vector point
     */
    public @NotNull VectorPoint point(@NotNull ModelLoadContext context, @NotNull Function<Vector3f, Vector3f> function) {
        return new VectorPoint(
                point().toFunction(context).map(function).memoize(),
                time(),
                new VectorPoint.BezierConfig(
                        Optional.ofNullable(bezierLeftTime).map(Float3::toVector).orElse(null),
                        Optional.ofNullable(bezierLeftValue).map(Float3::toVector).map(function).orElse(null),
                        Optional.ofNullable(bezierRightTime).map(Float3::toVector).orElse(null),
                        Optional.ofNullable(bezierRightValue).map(Float3::toVector).map(function).orElse(null)
                ),
                interpolation()
        );
    }

    /**
     * Gets interpolation
     * @return interpolation
     */
    @Override
    public @NotNull VectorInterpolator interpolation() {
        return interpolation != null ? interpolation : VectorInterpolator.LINEAR;
    }

    /**
     * Gets channel
     * @return channel
     */
    @Override
    public @NotNull KeyframeChannel channel() {
        return channel != null ? channel : KeyframeChannel.NOT_FOUND;
    }
}
