package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;

public interface Timed extends Comparable<Timed> {
    default int compareTo(@NotNull Timed o) {
        return Float.compare(time(), o.time());
    }

    float time();
}
