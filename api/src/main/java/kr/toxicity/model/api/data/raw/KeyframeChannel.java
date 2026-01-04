/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents the type of property a keyframe affects.
 *
 * @since 1.15.2
 */
@ApiStatus.Internal
public enum KeyframeChannel {
    /**
     * Affects the position of a bone.
     * @since 1.15.2
     */
    @SerializedName("position")
    POSITION,
    /**
     * Affects the rotation of a bone.
     * @since 1.15.2
     */
    @SerializedName("rotation")
    ROTATION,
    /**
     * Affects the scale of a bone.
     * @since 1.15.2
     */
    @SerializedName("scale")
    SCALE,
    /**
     * Represents a timeline for sound or particle effects.
     * @since 1.15.2
     */
    @SerializedName("timeline")
    TIMELINE,
    /**
     * Represents a sound effect.
     * @since 1.15.2
     */
    @SerializedName("sound")
    SOUND,
    /**
     * Represents a particle effect.
     * @since 1.15.2
     */
    @SerializedName("particle")
    PARTICLE,
    /**
     * Represents an unknown or unsupported channel.
     * @since 1.15.2
     */
    NOT_FOUND
}
