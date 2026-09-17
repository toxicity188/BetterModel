/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides scheduler contracts used by BetterModel tasks.
 * <p>
 * The scheduler API abstracts synchronous, asynchronous, delayed, and repeating
 * task execution so core logic can run on different server platforms through a
 * common contract.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ModelTask task = scheduler.async(() -> reloadInfo);
 * task.cancel();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.scheduler;
