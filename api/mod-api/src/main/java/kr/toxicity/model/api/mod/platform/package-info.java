/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides mod-platform implementations of shared platform wrappers.
 * <p>
 * Types in this package adapt mod runtime worlds, regions, locations, entities,
 * players, item stacks, and the platform adapter itself to the base BetterModel
 * platform contracts. Mod-specific classes stay here so the generic API remains
 * independent from a concrete mod loader or Minecraft runtime type.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ModAdapter adapter = ...;
 * ModEntity entity = adapter.entity(minecraftEntity);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.mod.platform;
