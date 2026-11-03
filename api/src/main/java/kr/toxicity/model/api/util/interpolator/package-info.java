/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides vector interpolation contracts for animation evaluation.
 * <p>
 * Interpolators in this package define how vector keyframe values are blended
 * between timed points. They are shared by animation data and runtime evaluation
 * code that needs consistent movement curves.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * VectorInterpolator interpolator = VectorInterpolator.LINEAR;
 * var value = interpolator.interpolate(points, 1, 0.5F);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.util.interpolator;
