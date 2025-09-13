package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.util.PackUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Parsed BlockBench model
 * @param name model name
 * @param resolution resolution
 * @param textures textures
 * @param group children group
 * @param animations animations
 */
@ApiStatus.Internal
public record ModelBlueprint(
        @NotNull String name,
        @NotNull ModelResolution resolution,
        @NotNull List<BlueprintTexture> textures,
        @NotNull List<BlueprintChildren> group,
        @NotNull Map<String, BlueprintAnimation> animations
) {

    /**
     * Checks this blueprint has textures.
     * @return has textures
     */
    public boolean hasTexture() {
        return textures.stream().anyMatch(BlueprintTexture::canBeRendered);
    }

    /**
     * Builds blueprint image
     * @return images
     */
    @NotNull
    @Unmodifiable
    public Stream<BlueprintImage> buildImage() {
        return textures.stream()
                .filter(BlueprintTexture::canBeRendered)
                .map(texture -> new BlueprintImage(
                        PackUtil.toPackName(name + "_" + texture.name()),
                        texture.image(),
                        texture.isAnimatedTexture() ? texture.toMcmeta() : null)
                );
    }
}
