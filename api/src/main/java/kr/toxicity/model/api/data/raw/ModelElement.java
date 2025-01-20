package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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
public record ModelElement(
        @NotNull String name,
        @NotNull UUID uuid,
        @NotNull Float3 from,
        @NotNull Float3 to,
        float inflate,
        @Nullable Float3 rotation,
        @NotNull Float3 origin,
        @NotNull ModelFace faces
) {
    public float max() {
        return to.minus(from).toVector().length();
    }
    public boolean hasTexture() {
        return faces.hasTexture();
    }
}
