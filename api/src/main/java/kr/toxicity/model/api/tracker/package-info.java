/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines tracker contracts for binding models to entities or dummy locations.
 * <p>
 * Trackers own runtime model state such as render pipelines, animation playback,
 * scale, rotation, hide options, update actions, and entity registries. The
 * package provides API-level control over model instances while delegating packet
 * emission and platform integration to lower layers.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * EntityTrackerRegistry registry = BetterModel.registry(entity).orElseThrow();
 * EntityTracker tracker = registry.tracker("example_model");
 * tracker.animate("walk");
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.tracker;
