package kr.toxicity.model.api.util;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record VectorPair(@NotNull Vector3f min, @NotNull Vector3f max) {
}
