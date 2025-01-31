package kr.toxicity.model.api.nms;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PacketBundler {
    boolean isEmpty();
    @NotNull PacketBundler copy();
    void send(@NotNull Player player);
}
