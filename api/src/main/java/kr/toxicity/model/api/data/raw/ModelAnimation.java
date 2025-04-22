package kr.toxicity.model.api.data.raw;

import kr.toxicity.model.api.animation.AnimationIterator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Raw animation of a model.
 * @param name name
 * @param loop whether to loop
 * @param uuid uuid
 * @param override override
 * @param length keyframe length
 * @param animators animators
 */
@ApiStatus.Internal
public record ModelAnimation(
        @NotNull String name,
        @NotNull AnimationIterator.Type loop,
        boolean override,
        @NotNull String uuid,
        float length,
        @Nullable Map<String, ModelAnimator> animators
) {
    @Override
    @NotNull
    public Map<String, ModelAnimator> animators() {
        return animators != null ? animators : Collections.emptyMap();
    }
}
