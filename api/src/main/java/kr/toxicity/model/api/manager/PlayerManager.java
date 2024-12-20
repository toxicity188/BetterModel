package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.nms.PlayerChannelHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlayerManager extends GlobalManager {
    @Nullable PlayerChannelHandler player(@NotNull UUID uuid);
    @NotNull PlayerChannelHandler player(@NotNull Player player);
}
