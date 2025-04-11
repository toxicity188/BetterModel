package kr.toxicity.model.api.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public final class EventUtil {
    private EventUtil() {
        throw new RuntimeException();
    }

    public static boolean call(@NotNull Event event) {
        Bukkit.getPluginManager().callEvent(event);
        return !(event instanceof Cancellable cancellable) || !cancellable.isCancelled();
    }
}
