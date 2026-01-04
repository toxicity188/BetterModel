/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static kr.toxicity.model.api.util.CollectionUtil.associate;

/**
 * Represents animation variable placeholders defined in a model file.
 * <p>
 * These placeholders allow for defining reusable values or expressions that can be referenced within Molang scripts in animations.
 * </p>
 *
 * @param variables a map of placeholder variable names to their string values
 * @since 1.15.2
 */
public record ModelPlaceholder(
    @NotNull @Unmodifiable Map<String, String> variables
) {
    /**
     * An empty placeholder instance with no variables.
     * @since 1.15.2
     */
    public static final ModelPlaceholder EMPTY = new ModelPlaceholder(Collections.emptyMap());

    /**
     * A JSON deserializer for parsing placeholders from a multi-line string.
     * @since 1.15.2
     */
    public static final JsonDeserializer<ModelPlaceholder> PARSER = (json, typeOfT, context) -> new ModelPlaceholder(associate(
        Arrays.stream(json.getAsString().split("\n"))
            .map(line -> line.split("=", 2))
            .filter(array -> array.length == 2),
        array -> array[0].trim(),
        array -> array[1].trim()
    ));

    /**
     * Replaces all placeholder variables in a given expression with their corresponding values.
     *
     * @param expression the expression containing placeholders
     * @return the expression with placeholders substituted
     * @since 1.15.2
     */
    public @NotNull String parseVariable(@NotNull String expression) {
        for (var entry : variables.entrySet()) {
            expression = expression.replace(entry.getKey(), entry.getValue());
        }
        return expression;
    }
}
