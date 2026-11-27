/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation;

/**
 * Tracks whether an overriding animation has matched a bone along the current branch.
 */
public enum AnimationOverrideState {
    /**
     * No bone on the current branch has matched yet.
     */
    NOT_MATCHED,
    /**
     * This bone or an ancestor has matched.
     */
    MATCHED
    ;

    /**
     * Checks whether an overriding animation can skip a bone without a matching track.
     *
     * @return {@code true} while the branch has no match
     */
    public boolean shouldSkip() {
        return this == NOT_MATCHED;
    }
}
