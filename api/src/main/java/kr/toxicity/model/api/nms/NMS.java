package kr.toxicity.model.api.nms;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface NMS {
    @NotNull ModelDisplay create(@NotNull Location location);
    @NotNull PlayerChannelHandler inject(@NotNull Player player);
    @NotNull PacketBundler createBundler();
}
