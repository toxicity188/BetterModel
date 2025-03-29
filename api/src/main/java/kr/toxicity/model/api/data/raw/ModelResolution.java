package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A texture resolution of model
 * @param width width
 * @param height height
 */
@ApiStatus.Internal
public record ModelResolution(
        int width,
        int height
) {
    /**
     * Selects correct resolution to use
     * @param other other resolution
     * @return correct resolution
     */
    public @NotNull ModelResolution then(@NotNull ModelResolution other) {
        return new ModelResolution(
                other.width > 0 ? other.width : width,
                other.height > 0 ? other.height : height
        );
    }
}
