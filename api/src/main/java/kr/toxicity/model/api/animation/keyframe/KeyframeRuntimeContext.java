/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import kr.toxicity.model.api.util.function.FloatSupplier;
import org.jetbrains.annotations.NotNull;

public interface KeyframeRuntimeContext {

    static @NotNull KeyframeRuntimeContext empty() {
        return Empty.INSTANCE;
    }

    default void set(@NotNull String key, float value) {}

    default void setFunction(@NotNull String key, FloatSupplier supplier) {}

    enum Empty implements KeyframeRuntimeContext {
        INSTANCE
    }
}
