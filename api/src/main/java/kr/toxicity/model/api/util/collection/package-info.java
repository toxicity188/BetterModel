/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains specialized collection helpers used by BetterModel APIs.
 * <p>
 * This package currently provides priority-aware maps and singleton sequenced
 * sets for places where standard collections need a small domain-specific shape
 * without adding a new dependency.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * PriorityMap<String, Runnable> listeners = new PriorityMap<>();
 * listeners.put("reload", action, 0);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.util.collection;
