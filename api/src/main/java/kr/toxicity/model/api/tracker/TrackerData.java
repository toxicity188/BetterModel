package kr.toxicity.model.api.tracker;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracker data
 * @param id model id
 * @param scaler scaler
 * @param rotator rotator
 * @param modifier modifier
 */
public record TrackerData(
        @NotNull String id,
        @Nullable ModelScaler scaler,
        @Nullable ModelRotator rotator,
        @NotNull TrackerModifier modifier
) {
    /**
     * Parser
     */
    public static final Gson PARSER = new GsonBuilder()
            .registerTypeAdapter(ModelScaler.class, (JsonDeserializer<ModelScaler>) (json, typeOfT, context) -> json.isJsonObject() ? ModelScaler.deserialize(json.getAsJsonObject()) : ModelScaler.defaultScaler())
            .registerTypeAdapter(ModelScaler.class, (JsonSerializer<ModelScaler>) (src, typeOfSrc, context) -> src.serialize())
            .registerTypeAdapter(ModelRotator.class, (JsonDeserializer<ModelRotator>) (json, typeOfT, context) -> json.isJsonObject() ? ModelRotator.deserialize(json.getAsJsonObject()) : ModelRotator.YAW)
            .registerTypeAdapter(ModelRotator.class, (JsonSerializer<ModelRotator>) (src, typeOfSrc, context) -> src.serialize())
            .create();

    /**
     * Serializes data as JSON
     * @return JSON element
     */
    public @NotNull JsonElement serialize() {
        return PARSER.toJsonTree(this);
    }

    /**
     * Deserializes data from JSON
     * @param element JSON element
     * @return tracker data
     */
    public static @NotNull TrackerData deserialize(@NotNull JsonElement element) {
        return element.isJsonPrimitive() ? new TrackerData(
                element.getAsString(),
                ModelScaler.entity(),
                ModelRotator.YAW,
                TrackerModifier.DEFAULT
        ) : PARSER.fromJson(element, TrackerData.class);
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
