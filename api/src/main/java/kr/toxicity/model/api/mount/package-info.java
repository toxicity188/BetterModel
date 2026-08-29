/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides model mount controller contracts.
 * <p>
 * Mount controllers define how rider and vehicle relationships are applied to
 * model trackers. Built-in controller access lives here so callers can choose
 * mount behavior without coupling to platform mount implementation details.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * MountController controller = MountControllers.WALK;
 * var canControl = controller.canControl();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.mount;
