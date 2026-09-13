/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines Bukkit event bridge contracts for BetterModel events.
 * <p>
 * This package adapts generic BetterModel event payloads to Bukkit plugin event
 * handling. It is intended for platform integration code that needs Bukkit
 * event lifecycle semantics while preserving the base API event model.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BetterModelBukkitEvent event = ...;
 * var modelEvent = event.modelEvent();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.bukkit.event;
