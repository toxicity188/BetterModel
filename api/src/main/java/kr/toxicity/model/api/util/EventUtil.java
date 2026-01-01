/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
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
    public static boolean call(@NotNull Event event) {
        Bukkit.getPluginManager().callEvent(event);
        return !(event instanceof Cancellable cancellable) || !cancellable.isCancelled();
    }
}
