/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains immutable configuration views exposed by BetterModel.
 * <p>
 * Configuration records in this package describe debug, indicator, module, and
 * resource-pack settings after they have been loaded by the implementation.
 * They are data contracts for consumers and should not perform file I/O.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * PackConfig pack = BetterModel.config().pack();
 * var namespace = pack.namespace();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.config;
