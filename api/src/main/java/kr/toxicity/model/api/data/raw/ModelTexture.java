package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * A raw model texture.
 * @param name texture's name
 * @param source texture's base64-encoded byte array
 * @param uvWidth uv-width
 * @param uvHeight uv-height
 */
public record ModelTexture(
        @NotNull String name,
        @NotNull String source,
        @SerializedName("uv_width") int uvWidth,
        @SerializedName("uv_height") int uvHeight
) {
}
