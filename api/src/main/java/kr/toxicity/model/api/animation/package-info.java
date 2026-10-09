/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines animation timing, interpolation, and runtime state contracts.
 * <p>
 * This package models keyframes, timed values, animation iterators, modifiers,
 * and state handlers that core implementations use to evaluate model motion.
 * API users can inspect or compose these contracts without depending on a
 * platform runtime.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * AnimationProgress progress = iterator.current();
 * var time = progress.time();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.animation;
