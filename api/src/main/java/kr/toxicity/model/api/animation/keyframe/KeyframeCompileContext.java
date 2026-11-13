/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

/**
 * Supplies the animation time used to resolve time queries when compiling a keyframe datapoint.
 * <pre>{@code
 * var compileContext = new KeyframeCompileContext(0.5F);
 * }</pre>
 *
 * @param animTime the animation time in seconds
 * @since 3.7.0
 */
public record KeyframeCompileContext(
    float animTime
) {
}
