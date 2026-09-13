/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides Bukkit-facing entry points for the BetterModel API.
 * <p>
 * This package focuses on the Bukkit platform implementation surface for the
 * generic API module. It exposes the Bukkit-specific access point and event bus
 * bridge used by Bukkit plugins that need BetterModel services while still
 * relying on the shared {@code kr.toxicity.model.api} contracts.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BetterModelBukkit api = ...;
 * BukkitModelEventBus events = api.eventBus();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.bukkit;
