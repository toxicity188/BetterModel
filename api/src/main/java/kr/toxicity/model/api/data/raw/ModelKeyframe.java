package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
