package kr.toxicity.model.api.tracker;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record TrackerData(
        @NotNull String id,
        @Nullable ModelScaler scaler,
        @Nullable ModelRotator rotator,
        @NotNull TrackerModifier modifier
) {
    public @NotNull JsonElement serialize() {
        return Tracker.PARSER.toJsonTree(this);
    }
    public static @NotNull TrackerData deserialize(@NotNull JsonElement element) {
        return element.isJsonPrimitive() ? new TrackerData(
                element.getAsString(),
                ModelScaler.entity(),
                ModelRotator.YAW,
                TrackerModifier.DEFAULT
        ) : Tracker.PARSER.fromJson(element, TrackerData.class);
    }

    @Override
    public @NotNull ModelScaler scaler() {
        return scaler != null ? scaler : ModelScaler.entity();
    }

    @Override
    public @NotNull ModelRotator rotator() {
        return rotator != null ? rotator : ModelRotator.YAW;
    }

    @NotNull
    @Override
    public String toString() {
        return serialize().toString();
    }
}
