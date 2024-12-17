package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ModelAnimator(
        @NotNull String name,
        @NotNull List<ModelKeyframe> keyframes
) {
}
