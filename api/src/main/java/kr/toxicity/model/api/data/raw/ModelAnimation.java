package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Raw animation of model.
 * @param name name
 * @param loop whether to loop
 * @param uuid uuid
 * @param length keyframe length
 * @param animators animators
 */
public record ModelAnimation(
        @NotNull String name,
        @NotNull String loop,
        @NotNull String uuid,
        float length,
        @NotNull Map<String, ModelAnimator> animators
) {
}
