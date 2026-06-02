/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains mod-platform scheduler integration for BetterModel tasks.
 * <p>
 * This package maps the shared scheduler contracts from the API module onto the
 * mod runtime task model. Mod platform implementations use it to run model work
 * through runtime-appropriate scheduling while returning generic task handles to
 * the rest of BetterModel.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ModModelScheduler scheduler = ...;
 * var task = scheduler.async(runnable);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.mod.scheduler;
