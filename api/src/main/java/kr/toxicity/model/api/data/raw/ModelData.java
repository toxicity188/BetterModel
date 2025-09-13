package kr.toxicity.model.api.data.raw;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static kr.toxicity.model.api.util.CollectionUtil.associate;
import static kr.toxicity.model.api.util.CollectionUtil.mapToList;

/**
 * Raw BlockBench model's data.
 * @param resolution resolution
 * @param elements elements
 * @param outliner children
 * @param textures textures
 * @param animations animations
 * @param placeholder placeholder
 */
@ApiStatus.Internal
public record ModelData(
        @NotNull ModelResolution resolution,
        @NotNull List<ModelElement> elements,
        @NotNull List<ModelChildren> outliner,
        @NotNull List<ModelTexture> textures,
        @Nullable List<ModelAnimation> animations,
        @Nullable @SerializedName("animation_variable_placeholders") ModelPlaceholder placeholder
) {
    /**
     * Gson parser
     */
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Float3.class, Float3.PARSER)
            .registerTypeAdapter(Float4.class, Float4.PARSER)
            .registerTypeAdapter(Datapoint.class, Datapoint.PARSER)
            .registerTypeAdapter(ModelChildren.class, ModelChildren.PARSER)
            .registerTypeAdapter(ModelPlaceholder.class, ModelPlaceholder.PARSER)
            .create();

    /**
     * Converts model data to blueprint
     * @param name blueprint name
     * @return blueprint
     */
    public @NotNull ModelBlueprint toBlueprint(@NotNull String name) {
        var placeholder = placeholder();
        var elementMap = associate(elements(), ModelElement::uuid);
        var group = mapToList(outliner(), children -> children.toBlueprint(elementMap));
        return new ModelBlueprint(
                name,
                resolution(),
                mapToList(textures(), ModelTexture::toBlueprint),
                group,
                associate(animations().stream().map(raw -> raw.toBlueprint(group, placeholder)), BlueprintAnimation::name)
        );
    }

    /**
     * Checks this model is supported in the Minecraft client.
     * @return is supported
     */
    public boolean isSupported() {
        return elements().stream().allMatch(ModelElement::isSupported);
    }


    /**
     * Gets placeholder
     * @return placeholder
     */
    @Override
    public @NotNull ModelPlaceholder placeholder() {
        return placeholder != null ? placeholder : ModelPlaceholder.EMPTY;
    }

    /**
     * Gets animation
     * @return animation
     */
    @Override
    @NotNull
    public List<ModelAnimation> animations() {
        return animations != null ? animations : Collections.emptyList();
    }
}
