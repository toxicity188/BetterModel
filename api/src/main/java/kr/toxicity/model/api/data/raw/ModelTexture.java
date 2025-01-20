package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public record ModelTexture(
        @NotNull String name,
        @NotNull String source,
        @SerializedName("uv_width") int uvWidth,
        @SerializedName("uv_height") int uvHeight
) {
}
