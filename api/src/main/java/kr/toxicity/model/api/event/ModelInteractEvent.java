package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * An interact event of damage event
 */
@Getter
public final class ModelInteractEvent extends PlayerEvent implements Cancellable {

    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    @Setter
    private boolean cancelled;
    private final @NotNull HitBox hitBox;
    private final @NotNull Hand hand;

    /**
     * Creates interact event
     * @param who player
     * @param hitBox hit-box
     * @param hand interacted hand
     */
    public ModelInteractEvent(@NotNull Player who, @NotNull HitBox hitBox, @NotNull Hand hand) {
        super(who);
        this.hitBox = hitBox;
        this.hand = hand;
    }

    /**
     * Hand
     */
    public enum Hand {
        /**
         * Off-hand
         */
        LEFT,
        /**
         * Main-hand
         */
        RIGHT
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
