package kr.toxicity.model.api.data.raw;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Raw BlockBench model's data.
 * @param resolution resolution
 * @param elements elements
 * @param outliner children
 * @param textures textures
 * @param animations animations
 */
@ApiStatus.Internal
public record ModelData(
        @NotNull ModelResolution resolution,
        @NotNull List<ModelElement> elements,
        @NotNull List<ModelChildren> outliner,
        @NotNull List<ModelTexture> textures,
        @Nullable List<ModelAnimation> animations
) {

    /**
     * Gets cube scale of this model
     * @return scale
     */
    public float scale() {
        return (float) elements().stream().mapToDouble(ModelElement::max).max().orElse(16F) / 16F;
    }

    @Override
    @NotNull
    public List<ModelAnimation> animations() {
        return animations != null ? animations : Collections.emptyList();
    }

    /**
     * Gson parser
     */
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Float3.class, Float3.PARSER)
            .registerTypeAdapter(Float4.class, Float4.PARSER)
            .registerTypeAdapter(Datapoint.class, Datapoint.PARSER)
            .registerTypeAdapter(KeyframeChannel.class, (JsonDeserializer<KeyframeChannel>) (json, typeOfT, context) -> KeyframeChannel.valueOf(json.getAsString().toUpperCase()))
            .registerTypeAdapter(ModelChildren.class, ModelChildren.PARSER)
            .create();
}
