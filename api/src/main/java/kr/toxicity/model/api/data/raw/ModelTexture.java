package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.NotNull;

public record ModelTexture(
        @NotNull String name,
        @NotNull String source
) {
}
