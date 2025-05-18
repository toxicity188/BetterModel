package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.ModelInteractionHand;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * An interacted event of damage event
 */
@Getter
public class ModelInteractEvent extends PlayerEvent implements Cancellable {

    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    @Setter
    private boolean cancelled;
    private final @NotNull HitBox hitBox;
    private final @NotNull ModelInteractionHand hand;

    /**
     * Creates interact event
     * @param who player
     * @param hitBox hit-box
     * @param hand interacted hand
     */
    @ApiStatus.Internal
    public ModelInteractEvent(@NotNull Player who, @NotNull HitBox hitBox, @NotNull ModelInteractionHand hand) {
        super(who);
        this.hitBox = hitBox;
        this.hand = hand;
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
