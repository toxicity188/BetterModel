package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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
    public @NotNull Float3 size() {
        return to.minus(from);
    }
    public float max() {
        return size().max();
    }
    public float min() {
        return size().min();
    }
    public boolean hasTexture() {
        return faces.hasTexture();
    }
}
