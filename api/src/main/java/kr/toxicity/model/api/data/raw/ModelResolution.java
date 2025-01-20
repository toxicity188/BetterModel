package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.NotNull;

public record ModelResolution(
        int width,
        int height
) {
    public @NotNull ModelResolution then(@NotNull ModelResolution other) {
        return new ModelResolution(
                other.width > 0 ? other.width : width,
                other.height > 0 ? other.height : height
        );
    }
}
