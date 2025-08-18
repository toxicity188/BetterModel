package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

/**
 * Blueprint json.
 * @param name json name
 * @param element json element
 */
public record BlueprintJson(
        @NotNull String name,
        @NotNull JsonElement element
) {
}
