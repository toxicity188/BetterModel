/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides Bukkit implementations of platform-neutral API wrappers.
 * <p>
 * Types in this package adapt Bukkit worlds, locations, entities, players, item
 * stacks, and the platform adapter itself to the shared BetterModel platform
 * contracts. This keeps Bukkit-specific runtime access in the Bukkit API module
 * instead of leaking Bukkit classes into the generic API contracts.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BukkitAdapter adapter = ...;
 * BukkitEntity entity = adapter.entity(bukkitEntity);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.bukkit.platform;
