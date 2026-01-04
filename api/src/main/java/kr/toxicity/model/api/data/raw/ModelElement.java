/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializer;
import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.Float3;
import kr.toxicity.model.api.data.blueprint.BlueprintElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a raw element within a model file.
 * <p>
 * This interface is a sealed type that permits specific implementations for different element types
 * found in BlockBench models, such as cubes, locators, null objects, and cameras.
 * </p>
 *
 * @since 1.15.2
 */
@ApiStatus.Internal
public sealed interface ModelElement {

    /**
     * A JSON deserializer that automatically dispatches to the correct {@link ModelElement} implementation based on the "type" field.
     * @since 1.15.2
     */
    JsonDeserializer<ModelElement> PARSER = (json, type, context) -> {
        var t = json.getAsJsonObject().getAsJsonPrimitive("type");
        var select = t != null ? t.getAsString() : "cube";
        return switch (select) {
            case "null_object" -> context.deserialize(json, NullObject.class);
            case "locator" -> context.deserialize(json, Locator.class);
            case "camera" -> context.deserialize(json, Camera.class);
            case "cube" -> context.deserialize(json, Cube.class);
            default -> new Unsupported(select);
        };
    };

    /**
     * Returns the unique identifier (UUID) of this element.
     *
     * @return the UUID string
     * @since 1.15.2
     */
    @NotNull String uuid();

    /**
     * Returns the type identifier of this element (e.g., "cube", "locator").
     *
     * @return the type string
     * @since 1.15.2
     */
    @NotNull String type();

    /**
     * Converts this raw element into a processed {@link BlueprintElement}.
     *
     * @return the blueprint element
     * @since 1.15.2
     */
    @NotNull BlueprintElement toBlueprint();

    /**
     * Checks if this element type is supported by the engine.
     *
     * @return true if supported, false otherwise
     * @since 1.15.2
     */
    default boolean isSupported() {
        return true;
    }

    /**
     * Represents a locator element, used for positioning attachments or particles.
     *
     * @param name the name of the locator
     * @param uuid the UUID of the locator
     * @param position the position of the locator
     * @since 1.15.2
     */
    record Locator(
        @NotNull String name,
        @NotNull String uuid,
        @Nullable Float3 position
    ) implements ModelElement {
        @Override
        public @NotNull String type() {
            return "locator";
        }
        /**
         * Returns the position of the locator.
         *
         * @return the position, or {@link Float3#ZERO} if not specified
         * @since 1.15.2
         */
        @Override
        public @NotNull Float3 position() {
            return position != null ? position : Float3.ZERO;
        }

        @Override
        public @NotNull BlueprintElement toBlueprint() {
            return new BlueprintElement.Locator(
                UUID.fromString(uuid),
                BoneTagRegistry.parse(name()),
                position()
            );
        }
    }

    /**
     * Represents a camera element (currently used as a placeholder).
     *
     * @param uuid the UUID of the camera
     * @since 1.15.2
     */
    record Camera(
        @NotNull String uuid
    ) implements ModelElement {
        @Override
        public @NotNull String type() {
            return "camera";
        }

        @Override
        public @NotNull BlueprintElement toBlueprint() {
            return new BlueprintElement.Camera(
                UUID.fromString(uuid)
            );
        }
    }

    /**
     * Represents a null object, often used for grouping or IK targets.
     *
     * @param name the name of the null object
     * @param uuid the UUID of the null object
     * @param ikTarget the UUID of the IK target, if any
     * @param ikSource the UUID of the IK source, if any
     * @param position the position of the null object
     * @since 1.15.2
     */
    record NullObject(
        @NotNull String name,
        @NotNull String uuid,
        @Nullable @SerializedName("ik_target") String ikTarget,
        @Nullable @SerializedName("ik_source") String ikSource,
        @Nullable Float3 position
    ) implements ModelElement {
        @Override
        public @NotNull String type() {
            return "null_object";
        }

        /**
         * Returns the position of the null object.
         *
         * @return the position, or {@link Float3#ZERO} if not specified
         * @since 1.15.2
         */
        @Override
        public @NotNull Float3 position() {
            return position != null ? position : Float3.ZERO;
        }

        @Override
        public @NotNull BlueprintElement toBlueprint() {
            return new BlueprintElement.NullObject(
                UUID.fromString(uuid),
                BoneTagRegistry.parse(name()),
                Optional.ofNullable(ikTarget())
                    .filter(str -> !str.isEmpty())
                    .map(UUID::fromString)
                    .orElse(null),
                Optional.ofNullable(ikSource())
                    .filter(str -> !str.isEmpty())
                    .map(UUID::fromString)
                    .orElse(null),
                position()
            );
        }
    }

    /**
     * Represents an unsupported element type.
     *
     * @param type the unsupported type string
     * @since 1.15.2
     */
    record Unsupported(@NotNull String type) implements ModelElement {

        @Override
        public @NotNull String uuid() {
            throw new UnsupportedOperationException(type());
        }

        @Override
        public boolean isSupported() {
            return false;
        }

        @Override
        public @NotNull BlueprintElement toBlueprint() {
            throw new UnsupportedOperationException(type());
        }
    }

    /**
     * Represents a standard cube element.
     *
     * @param name the name of the cube
     * @param uuid the UUID of the cube
     * @param from the starting coordinate (min corner)
     * @param to the ending coordinate (max corner)
     * @param inflate the inflation value (size increase)
     * @param rotation the rotation of the cube
     * @param origin the pivot point (origin) of the cube
     * @param faces the UV mapping for the faces
     * @param _visibility the visibility state (null means visible)
     * @since 1.15.2
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
         * Returns the starting coordinate (min corner).
         *
         * @return the from vector, or {@link Float3#ZERO} if not specified
         * @since 1.15.2
         */
        @Override
        public @NotNull Float3 from() {
            return from != null ? from : Float3.ZERO;
        }

        /**
         * Returns the ending coordinate (max corner).
         *
         * @return the to vector, or {@link Float3#ZERO} if not specified
         * @since 1.15.2
         */
        @Override
        public @NotNull Float3 to() {
            return to != null ? to : Float3.ZERO;
        }

        /**
         * Checks if the cube is visible.
         *
         * @return true if visible, false otherwise
         * @since 1.15.2
         */
        public boolean visibility() {
            return !Boolean.FALSE.equals(_visibility);
        }

        /**
         * Returns the rotation of the cube.
         *
         * @return the rotation, or {@link Float3#ZERO} if not specified
         * @since 1.15.2
         */
        @Override
        public @NotNull Float3 rotation() {
            return rotation != null ? rotation : Float3.ZERO;
        }

        @Override
        public @NotNull BlueprintElement toBlueprint() {
            return new BlueprintElement.Cube(
                name(),
                from(),
                to(),
                inflate(),
                rotation(),
                origin(),
                faces(),
                visibility()
            );
        }
    }
}
