/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.scheduler;

/**
 * A scheduled task of BetterModel
 */
public interface ModelTask {

    /**
     * Checks this task is canceled
     * @return whether to cancel
     */
    boolean isCancelled();

    /**
     * Cancels this task
     */
    void cancel();
}
