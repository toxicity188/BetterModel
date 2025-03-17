package kr.toxicity.model.api.data.raw;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Raw BlockBench model's data.
 * @param resolution resoltion
 * @param elements elements
 * @param outliner children
 * @param textures textures
 * @param animations animations
 */
public record ModelData(
        @NotNull ModelResolution resolution,
        @NotNull List<ModelElement> elements,
        @NotNull List<ModelChildren> outliner,
        @NotNull List<ModelTexture> textures,
        @Nullable List<ModelAnimation> animations
) {
    /**
     * Gson parser
     */
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Float3.class, (JsonDeserializer<Float3>) (json, typeOfT, context) -> Float3.PARSER.apply(json))
            .registerTypeAdapter(Float4.class, (JsonDeserializer<Float4>) (json, typeOfT, context) -> Float4.PARSER.apply(json))
            .registerTypeAdapter(Datapoint.class, (JsonDeserializer<Datapoint>) (json, typeOfT, context) -> Datapoint.PARSER.apply(json))
            .registerTypeAdapter(KeyframeChannel.class, (JsonDeserializer<KeyframeChannel>) (json, typeOfT, context) -> KeyframeChannel.valueOf(json.getAsString().toUpperCase()))
            .registerTypeAdapter(ModelChildren.class, (JsonDeserializer<ModelChildren>) (json, typeOfT, context) -> ModelChildren.PARSER.apply(json))
            .create();
}
