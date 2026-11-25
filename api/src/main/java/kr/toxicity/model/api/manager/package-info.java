/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Exposes manager contracts for loaded BetterModel runtime state.
 * <p>
 * Managers provide access to models, players, profiles, scripts, skins, and
 * reload information. The package defines the API surface that implementations
 * satisfy while keeping orchestration and storage details in core.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ModelManager models = BetterModel.platform().modelManager();
 * var renderer = models.model("example_model");
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.manager;
