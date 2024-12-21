package kr.toxicity.model.api.data.blueprint;

import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

public record NamedBoundingBox(@NotNull String name, @NotNull BoundingBox box) {
}
