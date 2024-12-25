package kr.toxicity.model.api.nms;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface TransformSupplier {
    TransformSupplier EMPTY = Vector3f::new;
    @NotNull Vector3f supplyTransform();
}
