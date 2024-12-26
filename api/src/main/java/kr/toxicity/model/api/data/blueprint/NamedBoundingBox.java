package kr.toxicity.model.api.data.blueprint;

import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record NamedBoundingBox(@NotNull String name, @NotNull BoundingBox box) {
    public @NotNull Vector3f centerVector() {
        return new Vector3f(
                (float) (box.getMaxX() + box.getMinX()),
                (float) (box.getMaxY() + box.getMinY()),
                (float) (box.getMaxZ() + box.getMinZ())
        ).div(2);
    }
    public @NotNull BoundingBox center() {
        var center = centerVector();
        return new BoundingBox(
                box.getMinX() - center.x,
                box.getMinY() - center.y,
                box.getMinZ() - center.z,
                box.getMaxX() - center.x,
                box.getMaxY() - center.y,
                box.getMaxZ() - center.z
        );
    }
}
