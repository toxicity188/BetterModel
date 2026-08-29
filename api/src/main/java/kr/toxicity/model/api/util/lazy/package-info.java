/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides lazy value helpers for float-oriented model calculations.
 * <p>
 * Lazy providers defer expensive or context-sensitive float evaluation until the
 * value is needed by animation or rendering code.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * LazyFloatProvider provider = new LazyFloatProvider(50.0F);
 * float value = provider.updateAndGet(1.0F);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.util.lazy;
