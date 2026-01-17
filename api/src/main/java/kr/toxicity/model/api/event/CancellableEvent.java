/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

public interface CancellableEvent extends ModelEvent {

    boolean isCancelled();

    void setCancelled(boolean cancel);
}
