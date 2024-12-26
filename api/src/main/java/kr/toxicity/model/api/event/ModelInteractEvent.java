package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class ModelInteractEvent extends PlayerEvent implements Cancellable {

    public static final HandlerList HANDLER_LIST = new HandlerList();

    @Setter
    private boolean cancelled;
    private final HitBox hitBox;
    private final Hand hand;

    public ModelInteractEvent(@NotNull Player who, @NotNull HitBox hitBox, @NotNull Hand hand) {
        super(who);
        this.hitBox = hitBox;
        this.hand = hand;
    }

    public enum Hand {
        LEFT,
        RIGHT
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
