package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A keyframe of model.
 * @param channel channel
 * @param dataPoints movement
 * @param time keyframe time
 */
public record ModelKeyframe(
        @NotNull KeyframeChannel channel,
        @NotNull @SerializedName("data_points") List<Datapoint> dataPoints,
        float time
) implements Comparable<ModelKeyframe> {
    @Override
    public int compareTo(@NotNull ModelKeyframe o) {
        return Float.compare(time, o.time);
    }
}
