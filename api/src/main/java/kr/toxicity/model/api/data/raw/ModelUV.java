package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ModelUV(
        @NotNull Float4 uv,
        float rotation,
        @Nullable String texture
) {
    public @NotNull JsonObject toJson(int resolution, int tint) {
        var object = new JsonObject();
        object.add("uv", uv.div((float) resolution).toJson());
        if (rotation != 0) object.addProperty("rotation", rotation);
        object.addProperty("tintindex", tint);
        object.addProperty("texture", "#" + texture);
        return object;
    }
}
