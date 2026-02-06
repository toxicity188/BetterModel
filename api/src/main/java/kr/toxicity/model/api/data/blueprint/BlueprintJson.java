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

    /**
     * Returns the name of the image file with a .json extension.
     *
     * @return the JSON file name
     * @since 2.0.1
     */
    public @NotNull String jsonName() {
        return name + ".json";
    }

    /**
     * Builds and returns the JSON content by invoking the supplier.
     *
     * @since 2.0.1
     * @return the generated JSON element
     */
    public @NotNull JsonElement buildJson() {
        return element.get();
    }
}
