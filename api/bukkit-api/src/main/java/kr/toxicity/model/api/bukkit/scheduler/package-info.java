/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains Bukkit scheduler integration for BetterModel tasks.
 * <p>
 * This package maps the shared scheduler contracts from the API module onto the
 * Bukkit scheduler model. Bukkit platform implementations use it to execute
 * model updates, asynchronous work, and delayed tasks through Bukkit-compatible
 * task handles.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BukkitModelScheduler scheduler = ...;
 * var task = scheduler.async(runnable);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.bukkit.scheduler;
