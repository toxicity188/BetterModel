package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.ApiStatus;

/**
 * keyframe channel.
 */
@ApiStatus.Internal
public enum KeyframeChannel {
    /**
     * Position
     */
    POSITION,
    /**
     * Rotation
     */
    ROTATION,
    /**
     * Scale
     */
    SCALE,
    /**
     * Timeline
     */
    TIMELINE,
    /**
     * Sound
     */
    SOUND,
    /**
     * Particle
     */
    PARTICLE
}
