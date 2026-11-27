/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.script;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Script parser
 */
@FunctionalInterface
public interface ScriptBuilder {
    /**
     * Build an entity script by data
     * @param data script data
     * @return script
     */
    @NotNull AnimationScript build(@NotNull ScriptData data);

    /**
     * Contains the arguments and metadata available to a script builder.
     *
     * @param args the raw argument text, or {@code null} if absent
     * @param metadata the properties from the model
     */
    record ScriptData(
        @Nullable String args,
        @NotNull ScriptMetaData metadata
    ) {}

    /**
     * Provides string properties from the model and typed access to their values.
     */
    interface ScriptMetaData {

        /**
         * Returns the script metadata properties.
         *
         * @return an unmodifiable map of property names to values
         */
        @NotNull @Unmodifiable
        Map<String, String> toMap();

        /**
         * Reads the exact strings {@code true} and {@code false} as Boolean values.
         *
         * @param key the property name
         * @return the parsed value, or {@code null} for a missing or unrecognized value
         */
        default @Nullable Boolean asBoolean(@NotNull String key) {
            var get = toMap().get(key);
            if (get == null) return null;
            return switch (get) {
                case "true" -> true;
                case "false" -> false;
                default -> null;
            };
        }

        /**
         * Parses a property as a {@link BigDecimal}.
         *
         * @param key the property name
         * @return the parsed decimal, or {@code null} for a missing or invalid value
         */
        default @Nullable Number asNumber(@NotNull String key) {
            var get = toMap().get(key);
            if (get == null) return null;
            try {
                return new BigDecimal(get);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /**
         * Looks up a property without conversion.
         *
         * @param key the property name
         * @return the property value, or {@code null} if absent
         */
        default @Nullable String asString(@NotNull String key) {
            return toMap().get(key);
        }
    }
}
