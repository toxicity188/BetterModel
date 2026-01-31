/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the persistent data state of a tracker.
 * <p>
 * This record holds configuration and state information such as model ID, scaling, rotation,
 * and visibility options, which can be serialized to JSON.
 * </p>
 *
 * @param id the model ID
 * @param scaler the model scaler
 * @param rotator the model rotator
 * @param modifier the tracker modifier
 * @param bodyRotator the body rotation data
 * @param hideOption the entity hide options
 * @param markForSpawn the set of player UUIDs marked for spawning
 * @since 1.15.2
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
     * The GSON parser for serializing and deserializing tracker data.
     * @since 1.15.2
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

    /**
     * Applies this data to an existing entity tracker.
     *
     * @param tracker the target tracker
     * @since 1.15.2
     */
    public void applyAs(@NotNull EntityTracker tracker) {
        tracker.markPlayerForSpawn(markForSpawn());
        tracker.hideOption(hideOption());
        tracker.scaler(scaler());
        tracker.rotator(rotator());
        tracker.bodyRotator().setValue(bodyRotator());
    }

    /**
     * Serializes this data to a JSON element.
     *
     * @return the JSON element
     * @since 1.15.2
     */
    public @NotNull JsonElement serialize() {
        return PARSER.toJsonTree(this);
    }

    /**
     * Deserializes tracker data from a JSON element.
     *
     * @param element the JSON element
     * @return the tracker data
     * @since 1.15.2
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

    /**
     * Returns the model scaler, or a default entity scaler if not specified.
     *
     * @return the model scaler
     * @since 1.15.2
     */
    @Override
    public @NotNull ModelScaler scaler() {
        return scaler != null ? scaler : ModelScaler.entity();
    }

    /**
     * Returns the model rotator, or a default YAW rotator if not specified.
     *
     * @return the model rotator
     * @since 1.15.2
     */
    @Override
    public @NotNull ModelRotator rotator() {
        return rotator != null ? rotator : ModelRotator.YAW;
    }

    /**
     * Returns the entity hide option, or the default hide option if not specified.
     *
     * @return the entity hide option
     * @since 1.15.2
     */
    @Override
    public @NotNull EntityHideOption hideOption() {
        return hideOption != null ? hideOption : EntityHideOption.DEFAULT;
    }

    /**
     * Returns the set of player UUIDs marked for spawning, or an empty set if not specified.
     *
     * @return the set of player UUIDs marked for spawning
     * @since 1.15.2
     */
    @Override
    public @NotNull Set<UUID> markForSpawn() {
        return markForSpawn != null ? markForSpawn : Collections.emptySet();
    }

    /**
     * Returns the body rotation data, or default body rotation data if not specified.
     *
     * @return the body rotation data
     * @since 1.15.2
     */
    @Override
    public @NotNull EntityBodyRotator.RotatorData bodyRotator() {
        return bodyRotator != null ? bodyRotator : EntityBodyRotator.defaultData();
    }

    /**
     * Serializes this TrackerData object to a JSON string.
     *
     * @return a JSON string representation of this object
     * @since 1.15.2
     */
    @NotNull
    @Override
    public String toString() {
        return serialize().toString();
    }
}
