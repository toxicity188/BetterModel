package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.Tracker;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class ModelSpawnAtPlayerEvent extends AbstractPlayerModelEvent implements Cancellable {
    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final Tracker tracker;
    private boolean cancelled;

    public ModelSpawnAtPlayerEvent(@NotNull Player player, @NotNull Tracker tracker) {
        super(player);
        this.tracker = tracker;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Gets handler list
     * @return handler list
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
