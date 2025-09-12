package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.ApiStatus;

/**
 * keyframe channel.
 */
@ApiStatus.Internal
public enum KeyframeChannel {
    /**
     * Position
     */
    @SerializedName("position")
    POSITION,
    /**
     * Rotation
     */
    @SerializedName("rotation")
    ROTATION,
    /**
     * Scale
     */
    @SerializedName("scale")
    SCALE,
    /**
     * Timeline
     */
    @SerializedName("timeline")
    TIMELINE,
    /**
     * Sound
     */
    @SerializedName("sound")
    SOUND,
    /**
     * Particle
     */
    @SerializedName("particle")
    PARTICLE,
    /**
     * Not found
     */
    NOT_FOUND
}
