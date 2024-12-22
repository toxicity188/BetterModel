package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public record ModelAnimation(
        @NotNull String name,
        @NotNull String loop,
        @NotNull UUID uuid,
        float length,
        @NotNull Map<UUID, ModelAnimator> animators
) {
}
