/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

@FunctionalInterface
public interface KeyframeOperator {

    KeyframeOperator ADD = (x, y, z, dest) -> dest.add(x, y, z);
    KeyframeOperator SCALE_MUL = (x, y, z, dest) -> dest.mul(x + 1F, y + 1F, z + 1F);
    KeyframeOperator SET = (x, y, z, dest) -> dest.set(x, y, z);

    @NotNull Vector3f operate(float x, float y, float z, @NotNull Vector3f dest);
}
