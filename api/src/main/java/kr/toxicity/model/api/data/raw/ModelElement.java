package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
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
 * @param _visibility visibility
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
        @NotNull ModelFace faces,
        @SerializedName("visibility") @Nullable Boolean _visibility
) {
    /**
     * Gets max length of this cube
     * @return cube length
     */
    public float max() {
        return to.minus(from).toVector().length();
    }

    public boolean visibility() {
        return !Boolean.FALSE.equals(_visibility);
    }

    @Override
    public @NotNull Float3 rotation() {
        return rotation != null ? rotation : Float3.ZERO;
    }

    /**
     * Checks this model has texture
     * @return model has texture
     */
    public boolean hasTexture() {
        return faces.hasTexture();
    }
}
