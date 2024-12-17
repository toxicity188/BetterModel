package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

public record BlueprintJson(
        @NotNull String name,
        @NotNull JsonElement element
) {
}
