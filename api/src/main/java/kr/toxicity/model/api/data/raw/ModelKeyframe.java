package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.animation.Timed;
import kr.toxicity.model.api.util.interpolator.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.util.Optional.ofNullable;

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
        @NotNull KeyframeChannel channel,
        @SerializedName("data_points") @NotNull List<Datapoint> dataPoints,
        @SerializedName("bezier_left_time") @Nullable Float3 bezierLeftTime,
        @SerializedName("bezier_left_value") @Nullable Float3 bezierLeftValue,
        @SerializedName("bezier_right_time") @Nullable Float3 bezierRightTime,
        @SerializedName("bezier_right_value") @Nullable Float3 bezierRightValue,
        @Nullable String interpolation,
        float time
) implements Timed {

    /**
     * Gets first data point
     * @return point
     */
    public @NotNull Datapoint point() {
        return dataPoints.getFirst();
    }

    /**
     * Finds proper interpolator matched by this keyframe
     * @return interpolator
     */
    public @NotNull VectorInterpolator findInterpolator() {
        if (interpolation == null) return VectorInterpolator.defaultInterpolator();
        return switch (interpolation.toLowerCase()) {
            case "linear" -> LinearInterpolator.INSTANCE;
            case "catmullrom" -> CatmullRomInterpolator.INSTANCE;
            case "step" -> StepInterpolator.INSTANCE;
            case "bezier" -> new BezierInterpolator(
                    ofNullable(bezierLeftTime).map(Float3::toVector).orElse(null),
                    ofNullable(bezierLeftValue).map(Float3::toVector).orElse(null),
                    ofNullable(bezierRightTime).map(Float3::toVector).orElse(null),
                    ofNullable(bezierRightValue).map(Float3::toVector).orElse(null)
            );
            default -> VectorInterpolator.defaultInterpolator();
        };
    }
}
