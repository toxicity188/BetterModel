/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines primitive and domain-specific functional interfaces.
 * <p>
 * These interfaces avoid unnecessary boxing for float-heavy animation code and
 * provide compact predicates or suppliers used by bone filtering, interpolation,
 * and script evaluation.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * FloatSupplier speed = FloatSupplier.of(1.0F);
 * Float2FloatFunction easing = Float2FloatFunction.of(0.5F);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.util.function;
