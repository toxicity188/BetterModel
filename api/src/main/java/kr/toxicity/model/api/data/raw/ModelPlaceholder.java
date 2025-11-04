/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
 * Model placeholder
 * @param variables variables
 */
public record ModelPlaceholder(
        @NotNull @Unmodifiable Map<String, String> variables
) {
    /**
     * Empty placeholder
     */
    public static final ModelPlaceholder EMPTY = new ModelPlaceholder(Collections.emptyMap());

    /**
     * Parser
     */
    public static final JsonDeserializer<ModelPlaceholder> PARSER = (json, typeOfT, context) -> new ModelPlaceholder(associate(
            Arrays.stream(json.getAsString().split("\n"))
                    .map(line -> line.split("=", 2))
                    .filter(array -> array.length == 2),
            array -> array[0].trim(),
            array -> array[1].trim()
    ));

    /**
     * Parses raw expression
     * @param expression expression
     * @return parsed expression
     */
    public @NotNull String parseVariable(@NotNull String expression) {
        for (var entry : variables.entrySet()) {
            expression = expression.replace(entry.getKey(), entry.getValue());
        }
        return expression;
    }
}
