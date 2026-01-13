/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelEventBus;
import kr.toxicity.model.api.event.CancellableEvent;
import kr.toxicity.model.api.event.ModelEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Event util
 */
@ApiStatus.Internal
public final class EventUtil {
    /**
     * No initializer
     */
    private EventUtil() {
        throw new RuntimeException();
    }

    /**
     * Calls this event
     * @return not canceled
     */
    @NotNull
    public static <T extends ModelEvent> BetterModelEventBus.Result call(@NotNull Class<T> eventClass, @NotNull Supplier<T> eventSupplier) {
        return BetterModel.eventBus().call(eventClass, eventSupplier);
    }
}
