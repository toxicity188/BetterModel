package kr.toxicity.model.api.tracker;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface Tracker extends AutoCloseable {
    @NotNull Location getLocation();
}
