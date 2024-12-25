package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class ModelDamagedEvent extends EntityEvent {
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final @NotNull HitBox hitBox;
    public ModelDamagedEvent(@NotNull HitBox hitBox) {
        super(hitBox.source());
        this.hitBox = hitBox;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
