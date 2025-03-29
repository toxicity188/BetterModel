package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A raw model animator.
 * @param name group name
 * @param keyframes keyframes
 */
@ApiStatus.Internal
public record ModelAnimator(
        @NotNull String name,
        @NotNull List<ModelKeyframe> keyframes
) {
}
