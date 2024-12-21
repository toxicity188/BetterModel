package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public record ModelFace(
        @NotNull ModelUV north,
        @NotNull ModelUV east,
        @NotNull ModelUV south,
        @NotNull ModelUV west,
        @NotNull ModelUV up,
        @NotNull ModelUV down
) {
    public @NotNull JsonObject toJson(int resolution, int tint) {
        var object = new JsonObject();
        if (north.texture() != null) object.add("north", north.toJson(resolution, tint));
        if (east.texture() != null) object.add("east", east.toJson(resolution, tint));
        if (south.texture() != null) object.add("south", south.toJson(resolution, tint));
        if (west.texture() != null) object.add("west", west.toJson(resolution, tint));
        if (up.texture() != null) object.add("up", up.toJson(resolution, tint));
        if (down.texture() != null) object.add("down", down.toJson(resolution, tint));
        return object;
    }

    public boolean hasTexture() {
        return north.texture() != null
                || east.texture() != null
                || south.texture() != null
                || west.texture() != null
                || up.texture() != null
                || down.texture() != null;
    }
}
