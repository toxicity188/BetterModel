package kr.toxicity.model.api.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Player-related event
 */
@Getter
public abstract class AbstractPlayerModelEvent extends AbstractModelEvent {

    private final Player player;

    /**
     * Creates with player
     * @param player player
     */
    @ApiStatus.Internal
    public AbstractPlayerModelEvent(@NotNull Player player) {
        this.player = player;
    }
}
