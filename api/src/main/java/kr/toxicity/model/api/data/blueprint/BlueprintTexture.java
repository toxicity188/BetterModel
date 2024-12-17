package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelTexture;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

public record BlueprintTexture(
        @NotNull String name,
        byte[] image
) {
    public static @NotNull BlueprintTexture from(@NotNull ModelTexture blueprint) {
        return new BlueprintTexture(
                blueprint.name().split("\\.")[0],
                Base64.getDecoder().decode(blueprint.source().split(",")[1])
        );
    }
}
