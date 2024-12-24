package kr.toxicity.model.api.nms;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PacketBundler {
    boolean isEmpty();
    void send(@NotNull Player player);
}
