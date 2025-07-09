package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;

public record RunningAnimation(@NotNull String name, @NotNull AnimationIterator.Type type) {

}