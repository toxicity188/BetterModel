/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import kr.toxicity.model.api.event.CancellableEvent;
import kr.toxicity.model.api.event.ModelEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

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
     * @param event event
     * @return not canceled
     */
    public static boolean call(@NotNull ModelEvent event) {
        Bukkit.getPluginManager().callEvent(event);
        return !(event instanceof CancellableEvent cancellable) || !cancellable.isCancelled();
    }
}
