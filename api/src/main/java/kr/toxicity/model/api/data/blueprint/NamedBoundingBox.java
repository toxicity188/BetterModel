package kr.toxicity.model.api.data.blueprint;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record NamedBoundingBox(@NotNull String name, @NotNull ModelBoundingBox box) {
    public @NotNull Vector3f centerVector() {
        return new Vector3f(
                (float) (box.maxX() + box.minX()),
                (float) (box.maxY() + box.minY()),
                (float) (box.maxZ() + box.minZ())
        ).div(2);
    }
    public @NotNull ModelBoundingBox center() {
        var center = centerVector();
        return new ModelBoundingBox(
                box.minX() - center.x,
                box.minY() - center.y,
                box.minZ() - center.z,
                box.maxX() - center.x,
                box.maxY() - center.y,
                box.maxZ() - center.z
        );
    }
}
