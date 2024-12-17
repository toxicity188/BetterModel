package kr.toxicity.model.api.nms;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface NMS {
    @NotNull ModelDisplay create(@NotNull Location location);
}
