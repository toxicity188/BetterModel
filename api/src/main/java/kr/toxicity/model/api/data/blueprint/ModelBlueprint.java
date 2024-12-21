package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelChildren;
import kr.toxicity.model.api.data.raw.ModelData;
import kr.toxicity.model.api.data.raw.ModelElement;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ModelBlueprint(
        @NotNull String name,
        double scale,
        int resolution,
        @NotNull List<BlueprintTexture> textures,
        @NotNull List<BlueprintChildren> group,
        @NotNull Map<String, BlueprintAnimation> animations
) {
    public static @NotNull ModelBlueprint from(@NotNull String name, @NotNull ModelData data) {
        var elementMap = data.elements().stream().collect(Collectors.toMap(ModelElement::uuid, e -> e));
        var scale = 48.0 / data.elements().stream().mapToDouble(ModelElement::max).max().orElseThrow();
        var list = new ArrayList<BlueprintChildren>();
        for (ModelChildren modelChildren : data.outliner()) {
            list.add(BlueprintChildren.from(modelChildren, elementMap));
        }
        return new ModelBlueprint(
                name,
                scale,
                Math.max(data.resolution().width(), data.resolution().height()) / 16,
                data.textures().stream().map(BlueprintTexture::from).toList(),
                list,
                data.animations().stream().map(BlueprintAnimation::from).collect(Collectors.toMap(BlueprintAnimation::name, a -> a))
        );
    }

    public @NotNull List<BlueprintJson> buildJson(int tint) {
        var list = new ArrayList<BlueprintJson>();
        for (BlueprintChildren blueprintChildren : group) {
            switch (blueprintChildren) {
                case BlueprintChildren.BlueprintElement blueprintElement -> {
                }
                case BlueprintChildren.BlueprintGroup blueprintGroup -> blueprintGroup.buildJson(tint,this, list);
            }
        }
        return list;
    }

    public @NotNull List<BlueprintImage> buildImage() {
        var list = new ArrayList<BlueprintImage>();
        for (BlueprintTexture texture : textures) {
            try (
                    var source = new ByteArrayInputStream(texture.image());
                    var buffer = new BufferedInputStream(source)
            ) {
                list.add(new BlueprintImage(name + "_" + texture.name(), ImageIO.read(buffer)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return list;
    }
}
