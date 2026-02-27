/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

public enum AnimationOverrideState {
    NOT_MATCHED,
    MATCHED
    ;

    public boolean shouldSkip() {
        return this == NOT_MATCHED;
    }
}
