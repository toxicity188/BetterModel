package kr.toxicity.model.api.tracker;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

public record TrackerData(@NotNull String id, @NotNull TrackerModifier modifier) {
    public @NotNull JsonElement serialize() {
        return Tracker.PARSER.toJsonTree(this);
    }
    public static @NotNull TrackerData deserialize(@NotNull JsonElement element) {
        return element.isJsonPrimitive() ? new TrackerData(element.getAsString(), TrackerModifier.DEFAULT) : Tracker.PARSER.fromJson(element, TrackerData.class);
    }

    @NotNull
    @Override
    public String toString() {
        return serialize().toString();
    }
}
