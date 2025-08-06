package kr.toxicity.model.api.nms;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ModelNametag {
    void alwaysVisible(boolean alwaysVisible);
    void component(@Nullable Component component);
    void teleport(@NotNull Location location);
    void send(@NotNull Player player);
    void remove(@NotNull PacketBundler bundler);
}
