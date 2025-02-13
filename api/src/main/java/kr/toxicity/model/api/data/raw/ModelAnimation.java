package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
        @Nullable Map<String, ModelAnimator> animators
) {
}
