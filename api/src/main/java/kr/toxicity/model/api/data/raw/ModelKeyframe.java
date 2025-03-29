package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A keyframe of model.
 * @param channel channel
 * @param dataPoints movement
 * @param time keyframe time
 */
@ApiStatus.Internal
public record ModelKeyframe(
        @NotNull KeyframeChannel channel,
        @NotNull @SerializedName("data_points") List<Datapoint> dataPoints,
        @Nullable String interpolation,
        float time
) implements Comparable<ModelKeyframe> {

    public @NotNull ModelKeyframe time(float time) {
        return new ModelKeyframe(
                channel,
                dataPoints,
                interpolation,
                time
        );
    }

    @Override
    public int compareTo(@NotNull ModelKeyframe o) {
        return Float.compare(time, o.time);
    }
}
