/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides the root contracts for accessing BetterModel services.
 * <p>
 * This package defines the stable entry points used by API consumers to reach the
 * active platform, model managers, evaluator, logger, configuration, and event bus.
 * It intentionally contains service contracts and facade methods only; platform
 * implementations and runtime wiring belong to core or platform-specific modules.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * var renderer = BetterModel.model("example_model");
 * var platform = BetterModel.platform();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api;
