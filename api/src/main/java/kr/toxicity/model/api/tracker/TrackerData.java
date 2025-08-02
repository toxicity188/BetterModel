package kr.toxicity.model.api.tracker;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Tracker data
 * @param id model id
 * @param scaler scaler
 * @param rotator rotator
 * @param modifier modifier
 * @param hideOption hide option
 */
public record TrackerData(
        @NotNull String id,
        @Nullable ModelScaler scaler,
        @Nullable ModelRotator rotator,
        @NotNull TrackerModifier modifier,
        @Nullable @SerializedName("body-rotator") EntityBodyRotator.RotatorData bodyRotator,
        @Nullable @SerializedName("hide-option") EntityHideOption hideOption,
        @Nullable @SerializedName("mark-for-spawn") Set<UUID> markForSpawn
) {
    /**
     * Parser
     */
    public static final Gson PARSER = new GsonBuilder()
            .registerTypeAdapter(ModelScaler.class, (JsonDeserializer<ModelScaler>) (json, typeOfT, context) -> json.isJsonObject() ? ModelScaler.deserialize(json.getAsJsonObject()) : ModelScaler.defaultScaler())
            .registerTypeAdapter(ModelScaler.class, (JsonSerializer<ModelScaler>) (src, typeOfSrc, context) -> src.serialize())
            .registerTypeAdapter(ModelRotator.class, (JsonDeserializer<ModelRotator>) (json, typeOfT, context) -> json.isJsonObject() ? ModelRotator.deserialize(json.getAsJsonObject()) : ModelRotator.YAW)
            .registerTypeAdapter(ModelRotator.class, (JsonSerializer<ModelRotator>) (src, typeOfSrc, context) -> src.serialize())
            .registerTypeAdapter(EntityHideOption.class, (JsonDeserializer<EntityHideOption>) (json, typeOfT, context) -> json.isJsonArray() ? EntityHideOption.deserialize(json.getAsJsonArray()) : EntityHideOption.DEFAULT)
            .registerTypeAdapter(EntityHideOption.class, (JsonSerializer<EntityHideOption>) (src, typeOfSrc, context) -> src.serialize())
            .registerTypeAdapter(UUID.class, (JsonDeserializer<UUID>) (json, typeOfT, context) -> UUID.fromString(json.getAsString()))
            .registerTypeAdapter(UUID.class, (JsonSerializer<UUID>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
            .create();

    public void applyAs(@NotNull EntityTracker tracker) {
        tracker.markPlayerForSpawn(markForSpawn());
        tracker.hideOption(hideOption());
        tracker.scaler(scaler());
        tracker.rotator(rotator());
        tracker.bodyRotator().setValue(bodyRotator());
    }

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
                null,
                TrackerModifier.DEFAULT,
                EntityBodyRotator.defaultData(),
                null,
                null
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

    @Override
    public @NotNull EntityHideOption hideOption() {
        return hideOption != null ? hideOption : EntityHideOption.DEFAULT;
    }

    @Override
    public @NotNull Set<UUID> markForSpawn() {
        return markForSpawn != null ? markForSpawn : Collections.emptySet();
    }

    @Override
    public @NotNull EntityBodyRotator.RotatorData bodyRotator() {
        return bodyRotator != null ? bodyRotator : EntityBodyRotator.defaultData();
    }

    @NotNull
    @Override
    public String toString() {
        return serialize().toString();
    }
}
