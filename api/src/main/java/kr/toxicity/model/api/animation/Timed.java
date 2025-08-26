package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;

/**
 * Object with keyframe time
 */
public interface Timed extends Comparable<Timed> {

    default int compareTo(@NotNull Timed o) {
        return Float.compare(time(), o.time());
    }

    /**
     * Gets time
     * @return time
     */
    float time();
}
