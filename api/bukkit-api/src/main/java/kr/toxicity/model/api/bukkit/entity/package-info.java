/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains Bukkit-backed entity wrappers for BetterModel.
 * <p>
 * The wrappers in this package adapt Bukkit entities and players to the
 * platform-neutral entity contracts from the base API module. They let model
 * trackers, renderers, and managers work with Bukkit runtime objects through the
 * shared {@code BaseEntity} and {@code BasePlayer} abstractions.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BaseBukkitEntity entity = ...;
 * var bukkitEntity = entity.entity();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.bukkit.entity;
