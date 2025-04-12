package kr.toxicity.model.api.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class AbstractPlayerModelEvent extends AbstractModelEvent {

    private final Player player;

    public AbstractPlayerModelEvent(@NotNull Player player) {
        this.player = player;
    }
}
