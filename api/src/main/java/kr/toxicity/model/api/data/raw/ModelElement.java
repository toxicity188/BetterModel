/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializer;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A raw model's element
 */
@ApiStatus.Internal
public sealed interface ModelElement {

    JsonDeserializer<ModelElement> PARSER = (json, type, context) -> {
        var t = json.getAsJsonObject().getAsJsonPrimitive("type");
        var select = t != null ? t.getAsString() : "cube";
        return switch (select) {
            case "null_object" -> context.deserialize(json, NullObject.class);
            case "locator" -> context.deserialize(json, Locator.class);
            case "cube" -> context.deserialize(json, Cube.class);
            default -> new Unsupported(select);
        };
    };

    /**
     * Gets name
     * @return name
     */
    @NotNull String name();

    /**
     * Gets uuid
     * @return uuid
     */
    @NotNull String uuid();

    /**
     * Gets type
     * @return type
     */
    @NotNull String type();

    /**
     * Checks this element is supported
     * @return supported
     */
    default boolean isSupported() {
        return true;
    }

    /**
     * A raw model's locator
     * @param name
     * @param uuid
     */
    record Locator(
            @NotNull String name,
            @NotNull String uuid
    ) implements ModelElement {
        @Override
        public @NotNull String type() {
            return "locator";
        }
    }

    /**
     * A raw model's null object
     * @param name
     * @param uuid
     * @param ikTarget ik target
     * @param position position
     */
    record NullObject(
            @NotNull String name,
            @NotNull String uuid,
            @NotNull @SerializedName("ik_target") String ikTarget,
            @Nullable Float3 position
    ) implements ModelElement {
        @Override
        public @NotNull String type() {
            return "null_object";
        }

        /**
         * Gets position
         * @return position
         */
        @Override
        public @NotNull Float3 position() {
            return position != null ? position : Float3.ZERO;
        }
    }

    /**
     * Unsupported model's object
     * @param type unsupported type
     */
    record Unsupported(@NotNull String type) implements ModelElement {

        @Override
        public @NotNull String name() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull String uuid() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSupported() {
            return false;
        }
    }

    /**
     * A raw model's cube.
     * @param name name
     * @param uuid cube's uuid
     * @param from min-position
     * @param to max-position
     * @param inflate inflate
     * @param rotation rotation
     * @param origin origin
     * @param faces uv
     * @param _visibility visibility
     */
    record Cube(
            @NotNull String name,
            @NotNull String uuid,
            @Nullable Float3 from,
            @Nullable Float3 to,
            float inflate,
            @Nullable Float3 rotation,
            @NotNull Float3 origin,
            @Nullable ModelFace faces,
            @SerializedName("visibility") @Nullable Boolean _visibility
    ) implements ModelElement {

        @Override
        public @NotNull String type() {
            return "cube";
        }

        /**
         * Gets from-position
         * @return from-position
         */
        @Override
        public @NotNull Float3 from() {
            return from != null ? from : Float3.ZERO;
        }

        /**
         * Gets to-position
         * @return to-position
         */
        @Override
        public @NotNull Float3 to() {
            return to != null ? to : Float3.ZERO;
        }

        /**
         * Gets visibility
         * @return visibility
         */
        public boolean visibility() {
            return !Boolean.FALSE.equals(_visibility);
        }

        /**
         * Gets rotation
         * @return rotation
         */
        @Override
        public @NotNull Float3 rotation() {
            return rotation != null ? rotation : Float3.ZERO;
        }
    }
}
