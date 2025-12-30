/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import com.google.gson.JsonArray;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Configuration for hiding various visual aspects of an entity.
 * <p>
 * This record allows selective hiding of equipment, fire effects, the entity body itself, and glowing effects.
 * </p>
 *
 * @param equipment whether to hide equipment
 * @param fire whether to hide burning state
 * @param visibility whether to hide entity's body
 * @param glowing whether to hide entity's glowing state
 * @since 1.15.2
 */
public record EntityHideOption(
    boolean equipment,
    boolean fire,
    boolean visibility,
    boolean glowing
) {
    /**
     * Default option (hides everything).
     * @since 1.15.2
     */
    public static final EntityHideOption DEFAULT = new EntityHideOption(
        true,
        true,
        true,
        true
    );
    /**
     * Disabled option (hides nothing).
     * @since 1.15.2
     */
    public static final EntityHideOption FALSE = builder().build();

    /**
     * Composites multiple options into a single one using OR logic.
     *
     * @param options the stream of options
     * @return the composited option
     * @since 1.15.2
     */
    public static @NotNull EntityHideOption composite(@NotNull Stream<EntityHideOption> options) {
        return builder()
            .composite(options)
            .build();
    }

    /**
     * Deserializes hide option from a JSON array.
     *
     * @param array the JSON array
     * @return the option
     * @since 1.15.2
     */
    public static @NotNull EntityHideOption deserialize(@NotNull JsonArray array) {
        return new EntityHideOption(
            array.get(0).getAsBoolean(),
            array.get(1).getAsBoolean(),
            array.get(2).getAsBoolean(),
            array.get(3).getAsBoolean()
        );
    }

    /**
     * Serializes hide option to a JSON array.
     *
     * @return the JSON array
     * @since 1.15.2
     */
    public @NotNull JsonArray serialize() {
        var array = new JsonArray(4);
        array.add(equipment);
        array.add(fire);
        array.add(visibility);
        array.add(glowing);
        return array;
    }

    /**
     * Creates a new builder for {@link EntityHideOption}.
     *
     * @return the builder
     * @since 1.15.2
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EntityHideOption}.
     *
     * @since 1.15.2
     */
    public static final class Builder {
        private boolean equipment;
        private boolean fire;
        private boolean visibility;
        private boolean glowing;

        /**
         * Private initializer
         */
        private Builder() {

        }

        /**
         * Composites multiple options into this builder.
         *
         * @param options the stream of options
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder composite(@NotNull Stream<EntityHideOption> options) {
            options.forEach(this::or);
            return this;
        }

        /**
         * Merges another hide option using OR logic.
         *
         * @param option the option to merge
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder or(@NotNull EntityHideOption option) {
            equipment = equipment || option.equipment;
            fire = fire || option.fire;
            visibility = visibility || option.visibility;
            glowing = glowing || option.glowing;
            return this;
        }

        /**
         * Builds the {@link EntityHideOption}.
         *
         * @return the created option
         * @since 1.15.2
         */
        public @NotNull EntityHideOption build() {
            return new EntityHideOption(
                equipment,
                fire,
                visibility,
                glowing
            );
        }
    }
}
