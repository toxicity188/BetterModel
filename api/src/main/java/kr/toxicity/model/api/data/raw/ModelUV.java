package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ModelUV(
        @NotNull Float4 uv,
        @Nullable String texture
) {
    public @NotNull JsonObject toJson(int resolution) {
        var object = new JsonObject();
        object.add("uv", uv.div((float) resolution).toJson());
        object.addProperty("texture", "#" + texture);
        return object;
    }
}
