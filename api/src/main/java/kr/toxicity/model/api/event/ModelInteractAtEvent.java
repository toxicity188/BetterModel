package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * An interacted event of damage event
 */
@Getter
public final class ModelInteractAtEvent extends ModelInteractEvent {

    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final Vector position;

    /**
     * Creates interact event
     * @param who player
     * @param hitBox hit-box
     * @param hand interacted hand
     */
    public ModelInteractAtEvent(@NotNull Player who, @NotNull HitBox hitBox, @NotNull Hand hand, @NotNull Vector position) {
        super(who, hitBox, hand);
        this.position = position;
    }


    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Gets a handler list
     * @return handler list
     */
    @SuppressWarnings("unused") //This method is necessary for event API.
    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
