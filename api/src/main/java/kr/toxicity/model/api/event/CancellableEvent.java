/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

/**
 * Represents an event that can be canceled.
 * <p>
 * Cancelling an event typically prevents the associated action from completing.
 * </p>
 *
 * @since 2.0.0
 */
public interface CancellableEvent extends ModelEvent {

    /**
     * Checks if the event has been canceled.
     *
     * @return true if canceled, false otherwise
     * @since 2.0.0
     */
    boolean isCancelled();

    /**
     * Sets the cancellation state of the event.
     *
     * @param cancel true to cancel the event, false to allow it
     * @since 2.0.0
     */
    void setCancelled(boolean cancel);
}
