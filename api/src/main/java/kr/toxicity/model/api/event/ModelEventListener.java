/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

/**
 * Represents a listener for model events.
 * <p>
 * This interface provides a mechanism to unregister the listener when it is no longer needed.
 * </p>
 *
 * @since 2.0.0
 */
public interface ModelEventListener {

    /**
     * A no-op listener implementation.
     * @since 2.0.0
     */
    ModelEventListener NONE = () -> {};

    /**
     * Unregisters this listener, stopping it from receiving further events.
     *
     * @since 2.0.0
     */
    void unregister();
}
