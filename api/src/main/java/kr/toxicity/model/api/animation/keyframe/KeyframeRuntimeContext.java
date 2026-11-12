/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import org.jetbrains.annotations.NotNull;

public interface KeyframeRuntimeContext {

    static @NotNull KeyframeRuntimeContext empty() {
        return Empty.INSTANCE;
    }

    enum Empty implements KeyframeRuntimeContext {
        INSTANCE
    }
}
