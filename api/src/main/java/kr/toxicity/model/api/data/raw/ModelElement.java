package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A raw model's element (cube).
 * @param name name
 * @param uuid cube's uuid
 * @param from min-position
 * @param to max-position
 * @param inflate inflate
 * @param rotation rotation
 * @param origin origin
 * @param faces uv
 */
@ApiStatus.Internal
public record ModelElement(
        @NotNull String name,
        @NotNull String uuid,
        @NotNull Float3 from,
        @NotNull Float3 to,
        float inflate,
        @Nullable Float3 rotation,
        @NotNull Float3 origin,
        @NotNull ModelFace faces
) {
    /**
     * Gets max length of this cube
     * @return cube length
     */
    public float max() {
        return to.minus(from).toVector().length();
    }

    /**
     * Checks this model has texture
     * @return model has texture
     */
    public boolean hasTexture() {
        return faces.hasTexture();
    }
}
