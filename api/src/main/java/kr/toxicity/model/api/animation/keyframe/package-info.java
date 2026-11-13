/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines animation keyframe values, interpolation points, and sampled timelines.
 *
 * <p>A {@link kr.toxicity.model.api.animation.keyframe.KeyframeDatapoint} represents a number
 * or a Molang expression. A {@link kr.toxicity.model.api.animation.keyframe.KeyframeCompileContext}
 * supplies animation time when that datapoint is compiled. A
 * {@link kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext} supplies query values
 * when the compiled keyframe is evaluated.</p>
 *
 * <p>A {@link kr.toxicity.model.api.animation.keyframe.KeyframePoint} associates a vector
 * datapoint with its time, axis mapping, and interpolation settings. A
 * {@link kr.toxicity.model.api.animation.keyframe.KeyframeTimeline} stores samples by index.
 * Constant timelines evaluate samples when they are appended. Runtime timelines keep the
 * compiled vectors so they can evaluate queries each time a sample is applied.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * var datapoints = KeyframeDatapointVector.of(
 *     KeyframeDatapoint.of(2F), KeyframeDatapoint.ZERO, KeyframeDatapoint.ZERO);
 * var vector = datapoints.compile(new KeyframeCompileContext(0.5F));
 *
 * var builder = KeyframeTimeline.constantBuilder(1);
 * builder.append(vector);
 * var timeline = builder.build();
 * var sample = timeline.apply(0, KeyframeRuntimeContext.empty(),
 *     new org.joml.Vector3f(), KeyframeOperator.SET);
 * }</pre>
 *
 * @since 3.7.0
 */
package kr.toxicity.model.api.animation.keyframe;
