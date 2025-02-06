package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelChildren;
import kr.toxicity.model.api.data.raw.ModelData;
import kr.toxicity.model.api.data.raw.ModelElement;
import kr.toxicity.model.api.data.raw.ModelResolution;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ModelBlueprint(
        @NotNull String name,
        double scale,
        @NotNull ModelResolution resolution,
        @NotNull List<BlueprintTexture> textures,
        @NotNull List<BlueprintChildren> group,
        @NotNull Map<String, BlueprintAnimation> animations
) {
    public static @NotNull ModelBlueprint from(@NotNull String name, @NotNull ModelData data) {
        var elementMap = data.elements().stream().collect(Collectors.toMap(ModelElement::uuid, e -> e));
        var scale = data.elements().stream().mapToDouble(ModelElement::max).max().orElseThrow() / 16;
        var list = new ArrayList<BlueprintChildren>();
        for (ModelChildren modelChildren : data.outliner()) {
            var children = BlueprintChildren.from(modelChildren, elementMap, (float) scale);
            list.add(children);
        }
        return new ModelBlueprint(
                name.toLowerCase(),
                scale,
                data.resolution(),
                data.textures().stream().map(BlueprintTexture::from).toList(),
                list,
                data.animations() == null ? Collections.emptyMap() : data.animations().stream().map(BlueprintAnimation::from).collect(Collectors.toMap(BlueprintAnimation::name, a -> a))
        );
    }

    public @NotNull List<BlueprintImage> buildImage() {
        var list = new ArrayList<BlueprintImage>();
        for (BlueprintTexture texture : textures) {
            list.add(new BlueprintImage(name + "_" + texture.name(), texture.image(), texture.isAnimatedTexture() ? texture.toMcmeta() : null));
        }
        return list;
    }
}
