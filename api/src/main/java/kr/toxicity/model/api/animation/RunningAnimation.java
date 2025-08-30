package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;

/**
 * Running animation
 * @param name name
 * @param type type
 */
public record RunningAnimation(@NotNull String name, @NotNull AnimationIterator.Type type) {
}