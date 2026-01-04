/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.script.BlueprintScript;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;

/**
 * Represents a complete, processed animation for a model.
 * <p>
 * This record contains all the necessary data to play an animation, including keyframes for each bone,
 * loop settings, and associated scripts.
 * </p>
 *
 * @param name the name of the animation
 * @param loop the default loop mode
 * @param length the length of the animation in seconds
 * @param override whether this animation overrides others
 * @param animator a map of animators for each bone
 * @param script the script associated with this animation, if any
 * @param emptyAnimator a list of empty movements, used as a fallback or for initialization
 * @since 1.15.2
 */
public record BlueprintAnimation(
    @NotNull String name,
    @NotNull AnimationIterator.Type loop,
    float length,
    boolean override,
    @NotNull @Unmodifiable Map<BoneName, BlueprintAnimator> animator,
    @Nullable BlueprintScript script,
    @NotNull List<AnimationMovement> emptyAnimator
) {

    /**
     * Retrieves the script for this animation, considering the provided modifier.
     * <p>
     * If the modifier overrides the animation or specifies a player, the script may be suppressed.
     * </p>
     *
     * @param modifier the animation modifier
     * @return the script, or null if suppressed
     * @since 1.15.2
     */
    public @Nullable BlueprintScript script(@NotNull AnimationModifier modifier) {
        return modifier.override(override) || modifier.player() != null ? null : script;
    }

    /**
     * Creates an iterator for the empty animation sequence.
     *
     * @param type the loop type
     * @return an animation iterator
     * @since 1.15.2
     */
    public @NotNull AnimationIterator<AnimationMovement> emptyIterator(@NotNull AnimationIterator.Type type) {
        return type.create(emptyAnimator);
    }
}
