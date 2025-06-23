package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelChildren;
import kr.toxicity.model.api.data.raw.ModelData;
import kr.toxicity.model.api.data.raw.ModelElement;
import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.util.PackUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parsed BlockBench model
 * @param name model name
 * @param scale model cube scale
 * @param resolution resolution
 * @param textures textures
 * @param group children group
 * @param animations animations
 */
@ApiStatus.Internal
public record ModelBlueprint(
        @NotNull String name,
        float scale,
        @NotNull ModelResolution resolution,
        @NotNull List<BlueprintTexture> textures,
        @NotNull List<BlueprintChildren> group,
        @NotNull Map<String, BlueprintAnimation> animations
) {

    /**
     * Creates blueprint with raw data
     * @param name blueprint name
     * @param data raw data
     * @return blueprint
     */
    public static @NotNull ModelBlueprint from(@NotNull String name, @NotNull ModelData data) {
        var elementMap = data.elements().stream().collect(Collectors.toUnmodifiableMap(ModelElement::uuid, e -> e));
        var list = new ArrayList<BlueprintChildren>();
        for (ModelChildren modelChildren : data.outliner()) {
            var children = BlueprintChildren.from(modelChildren, elementMap);
            list.add(children);
        }
        return new ModelBlueprint(
                name,
                data.scale(),
                data.resolution(),
                data.textures().stream().map(BlueprintTexture::from).toList(),
                list,
                data.animations().stream().map(BlueprintAnimation::from).collect(Collectors.toUnmodifiableMap(BlueprintAnimation::name, a -> a))
        );
    }

    /**
     * Builds blueprint image
     * @return images
     */
    public @NotNull List<BlueprintImage> buildImage() {
        var list = new ArrayList<BlueprintImage>();
        for (BlueprintTexture texture : textures) {
            list.add(new BlueprintImage(PackUtil.toPackName(name + "_" + texture.name()), texture.image(), texture.isAnimatedTexture() ? texture.toMcmeta() : null));
        }
        return list;
    }
}
