package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.tracker.ModelRotation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface HitBoxSource {
    @NotNull Vector3f hitBoxPosition();
    @NotNull ModelRotation hitBoxRotation();
}
