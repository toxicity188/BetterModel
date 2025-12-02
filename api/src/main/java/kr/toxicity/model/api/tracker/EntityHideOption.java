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
 * Entity hide option
 * @param equipment whether to hide equipment
 * @param fire whether to hide burning state
 * @param visibility whether to hide entity's body
 * @param glowing whether to hide entity's glowing state
 */
public record EntityHideOption(
    boolean equipment,
    boolean fire,
    boolean visibility,
    boolean glowing
) {
    /**
     * Default option
     */
    public static final EntityHideOption DEFAULT = new EntityHideOption(
        true,
        true,
        true,
        true
    );
    /**
     * Disabled option
     */
    public static final EntityHideOption FALSE = builder().build();

    /**
     * Composites all options to single one
     * @param options options
     * @return composited option
     */
    public static @NotNull EntityHideOption composite(@NotNull Stream<EntityHideOption> options) {
        return builder()
            .composite(options)
            .build();
    }

    /**
     * Deserializes hide option from JSON
     * @param array JSON array
     * @return option
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
     * Serializes hide option to JSON
     * @return JSON array
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
     * Creates builder of hide option.
     * @return builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Builder
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
         * Composites all options to this builder
         * @param options options
         * @return self
         */
        public @NotNull Builder composite(@NotNull Stream<EntityHideOption> options) {
            options.forEach(this::or);
            return this;
        }

        /**
         * Merges hide option as OR gate
         * @param option option
         * @return self
         */
        public @NotNull Builder or(@NotNull EntityHideOption option) {
            equipment = equipment || option.equipment;
            fire = fire || option.fire;
            visibility = visibility || option.visibility;
            glowing = glowing || option.glowing;
            return this;
        }

        /**
         * Builds hide option
         * @return hide option
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
