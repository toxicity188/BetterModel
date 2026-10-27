/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides shared utility helpers used by API contracts and implementations.
 * <p>
 * Utilities in this package cover collections, entity math, interpolation,
 * logging, HTTP access, pack helpers, reflection, event dispatch, and transformed
 * item stacks. They are small reusable helpers rather than extension points for
 * platform business logic.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * var value = MathUtil.clamp(input, 0.0F, 1.0F);
 * LogUtil.debug("Loaded model data");
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.util;
