package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.tracker.ModelRotation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Hit-box transformation source
 */
public interface HitBoxSource {
    /**
     * Gets hit-box position
     * @return position
     */
    @NotNull Vector3f hitBoxPosition();

    /**
     * Gets hit-box rotation
     * @return rotation
     */
    @NotNull ModelRotation hitBoxRotation();

    /**
     * Gets hit-box scale
     * @return scale
     */
    float hitBoxScale();
}
