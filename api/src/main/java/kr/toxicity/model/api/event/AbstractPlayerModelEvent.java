package kr.toxicity.model.api.event;

import kr.toxicity.model.api.BetterModel;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractPlayerModelEvent extends PlayerEvent {
    public AbstractPlayerModelEvent(@NotNull Player player) {
        super(player, !BetterModel.inst().nms().isSync());
    }
}
