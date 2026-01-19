/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelEventBus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a base event in the BetterModel system.
 * <p>
 * All events related to model lifecycle, interaction, and animation implement this interface.
 * Events can be dispatched using the {@link #call()} method.
 * </p>
 *
 * @since 2.0.0
 */
public interface ModelEvent {

    /**
     * Dispatches this event to the global event bus.
     *
     * @return the result of the event call
     * @since 2.0.0
     */
    default @NotNull BetterModelEventBus.Result call() {
        return BetterModel.eventBus().call(this);
    }
}
