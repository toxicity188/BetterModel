package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A raw model's element (cube).
 * @param name name
 * @param type type
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
        @Nullable Type type,
        @NotNull String uuid,
        @Nullable Float3 from,
        @Nullable Float3 to,
        float inflate,
        @Nullable Float3 rotation,
        @NotNull Float3 origin,
        @Nullable ModelFace faces,
        @SerializedName("visibility") @Nullable Boolean _visibility
) {
    /**
     * Gets max length of this cube
     * @param origin origin
     * @return cube length
     */
    public float max(@NotNull Float3 origin) {
        var f = from().minus(origin);
        var t = to().minus(origin);
        var max = 0F;
        max = Math.max(max, Math.abs(f.x()));
        max = Math.max(max, Math.abs(f.y()));
        max = Math.max(max, Math.abs(f.z()));
        max = Math.max(max, Math.abs(t.x()));
        max = Math.max(max, Math.abs(t.y()));
        max = Math.max(max, Math.abs(t.z()));
        return max;
    }

    /**
     * Gets from-position
     * @return from-position
     */
    @Override
    public @NotNull Float3 from() {
        return from != null ? from : Float3.ZERO;
    }

    /**
     * Gets to-position
     * @return to-position
     */
    @Override
    public @NotNull Float3 to() {
        return to != null ? to : Float3.ZERO;
    }

    /**
     * Gets visibility
     * @return visibility
     */
    public boolean visibility() {
        return !Boolean.FALSE.equals(_visibility);
    }

    /**
     * Gets rotation
     * @return rotation
     */
    @Override
    public @NotNull Float3 rotation() {
        return rotation != null ? rotation : Float3.ZERO;
    }

    /**
     * Gets element type
     * @return element type
     */
    @Override
    public @NotNull Type type() {
        return type != null ? type : Type.CUBE;
    }


    /**
     * Checks this model has texture
     * @return model has texture
     */
    public boolean hasTexture() {
        return faces != null && faces.hasTexture();
    }

    /**
     * Checks this element is supported in the Minecraft client.
     * @return supported
     */
    public boolean isSupported() {
        return type() == Type.CUBE;
    }

    /**
     * Element type
     */
    public enum Type {
        /**
         * Cube
         */
        @SerializedName("cube")
        CUBE,
        /**
         * Mesh
         */
        @SerializedName("mesh")
        MESH
    }
}
