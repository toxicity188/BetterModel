/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides platform-neutral entity and player abstractions.
 * <p>
 * These interfaces are the API-level wrappers used by trackers, managers, and
 * renderers when they need entity identity or player-specific state without
 * depending on Bukkit, Fabric, or Minecraft server internals.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BaseEntity entity = registry.entity();
 * var uniqueId = entity.uniqueId();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.entity;
