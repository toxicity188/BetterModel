package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelData;
import kr.toxicity.model.api.data.raw.ModelElement;
import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.util.PackUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;

import static kr.toxicity.model.api.util.CollectionUtil.*;

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
        var elementMap = associate(data.elements(), ModelElement::uuid, e -> e);
        return new ModelBlueprint(
                name,
                data.scale(),
                data.resolution(),
                mapToList(data.textures(), BlueprintTexture::from),
                mapToList(data.outliner(), children -> BlueprintChildren.from(children, elementMap)),
                associate(data.animations().stream().map(BlueprintAnimation::from), BlueprintAnimation::name, a -> a)
        );
    }

    /**
     * Builds blueprint image
     * @return images
     */
    @NotNull
    @Unmodifiable
    public List<BlueprintImage> buildImage() {
        return mapToList(textures, texture -> new BlueprintImage(PackUtil.toPackName(name + "_" + texture.name()), texture.image(), texture.isAnimatedTexture() ? texture.toMcmeta() : null));
    }
}
