/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Represents a JSON file to be generated as part of the resource pack.
 * <p>
 * This record holds the file name and a supplier for the JSON content.
 * </p>
 *
 * @param name the name of the JSON file (including extension)
 * @param element a supplier that provides the JSON content
 * @since 1.15.2
 */
public record BlueprintJson(
    @NotNull String name,
    @NotNull Supplier<JsonElement> element
) {
}
