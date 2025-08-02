package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.util.PackUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;

import static kr.toxicity.model.api.util.CollectionUtil.mapToList;

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
     * Builds blueprint image
     * @return images
     */
    @NotNull
    @Unmodifiable
    public List<BlueprintImage> buildImage() {
        return mapToList(textures, texture -> new BlueprintImage(
                PackUtil.toPackName(name + "_" + texture.name()),
                texture.image(),
                texture.isAnimatedTexture() ? texture.toMcmeta() : null)
        );
    }
}
