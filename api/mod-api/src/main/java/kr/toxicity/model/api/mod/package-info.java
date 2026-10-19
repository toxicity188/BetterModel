/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides mod-platform entry points for the BetterModel API.
 * <p>
 * This package focuses on the mod runtime implementation surface for the generic
 * API module. It exposes the mod-specific access point used by Fabric or similar
 * mod integrations while keeping consumers aligned with the shared BetterModel
 * contracts.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BetterModelMod api = ...;
 * var platform = api.platform();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.mod;
