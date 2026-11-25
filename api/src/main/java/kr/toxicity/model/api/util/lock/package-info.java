/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains lightweight lock helpers used by model state code.
 * <p>
 * Lock types provide simple single and duplex locking contracts for places that
 * need explicit state protection without exposing implementation-specific
 * synchronization details through higher-level APIs.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * SingleLock lock = new SingleLock();
 * var value = lock.accessToLock(() -> "model");
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.util.lock;
