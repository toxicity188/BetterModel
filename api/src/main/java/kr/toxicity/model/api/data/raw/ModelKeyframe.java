/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.animation.Timed;
import kr.toxicity.model.api.animation.keyframe.KeyframeAxis;
import kr.toxicity.model.api.animation.keyframe.KeyframePoint;
import kr.toxicity.model.api.data.Float3;
import kr.toxicity.model.api.util.interpolator.VectorInterpolator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Represents a single keyframe in an animation timeline.
 * <p>
 * A keyframe defines the state of a bone (position, rotation, or scale) at a specific time,
 * along with interpolation data (linear, catmull-rom, bezier) to smooth transitions.
 * </p>
 *
 * @param channel the channel this keyframe affects (position, rotation, scale)
 * @param dataPoints the list of data points (values) for this keyframe
 * @param bezierLeftTime the time offset for the left bezier handle
 * @param bezierLeftValue the value offset for the left bezier handle
 * @param bezierRightTime the time offset for the right bezier handle
 * @param bezierRightValue the value offset for the right bezier handle
 * @param interpolation the interpolation type (e.g., linear, catmullrom, bezier)
 * @param time the time of the keyframe in seconds
 * @since 1.15.2
 */
@ApiStatus.Internal
public record ModelKeyframe(
    @Nullable KeyframeChannel channel,
    @SerializedName("data_points") @NotNull List<ModelDatapoint> dataPoints,
    @SerializedName("bezier_left_time") @Nullable Float3 bezierLeftTime,
    @SerializedName("bezier_left_value") @Nullable Float3 bezierLeftValue,
    @SerializedName("bezier_right_time") @Nullable Float3 bezierRightTime,
    @SerializedName("bezier_right_value") @Nullable Float3 bezierRightValue,
    @Nullable VectorInterpolator interpolation,
    float time
) implements Timed {

    /**
     * Checks if this keyframe contains any data points.
     *
     * @return true if data points exist, false otherwise
     * @since 1.15.2
     */
    public boolean hasPoint() {
        return !dataPoints.isEmpty();
    }

    /**
     * Returns the first data point in the list.
     *
     * @return the first data point
     * @throws java.util.NoSuchElementException if the list is empty
     * @since 1.15.2
     */
    public @NotNull ModelDatapoint point() {
        return dataPoints.getFirst();
    }

    /**
     * Converts the first data point into an animation point with per-axis Bézier offsets.
     * Missing handle vectors default to zero.
     * <pre>{@code
     * var point = keyframe.point(context, axis);
     * }</pre>
     *
     * @param context the load context used to resolve placeholders in the data point
     * @param axis the axis mapping applied to the vector and Bézier value offsets
     * @return the animation point for this keyframe
     * @throws java.util.NoSuchElementException if this keyframe has no data points
     * @since 3.7.0
     */
    public @NotNull KeyframePoint point(@NotNull ModelLoadContext context, @NotNull KeyframeAxis axis) {
        return new KeyframePoint(
            point().toVector(context),
            axis,
            time(),
            new KeyframePoint.BezierConfig(
                Optional.ofNullable(bezierLeftTime).map(Float3::toVector).orElse(new Vector3f()),
                Optional.ofNullable(bezierLeftValue).map(Float3::toVector).map(axis).orElse(new Vector3f()),
                Optional.ofNullable(bezierRightTime).map(Float3::toVector).orElse(new Vector3f()),
                Optional.ofNullable(bezierRightValue).map(Float3::toVector).map(axis).orElse(new Vector3f())
            ),
            interpolation()
        );
    }

    /**
     * Returns the interpolation type for this keyframe.
     *
     * @return the interpolation type, defaulting to {@link VectorInterpolator#LINEAR} if null
     * @since 1.15.2
     */
    @Override
    public @NotNull VectorInterpolator interpolation() {
        return interpolation != null ? interpolation : VectorInterpolator.LINEAR;
    }

    /**
     * Returns the channel this keyframe affects.
     *
     * @return the channel, defaulting to {@link KeyframeChannel#NOT_FOUND} if null
     * @since 1.15.2
     */
    @Override
    public @NotNull KeyframeChannel channel() {
        return channel != null ? channel : KeyframeChannel.NOT_FOUND;
    }
}
