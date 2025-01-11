package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public final class ModelDamagedEvent extends EntityEvent implements Cancellable {

    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final @NotNull HitBox hitBox;
    private final ModelDamageSource source;

    private float damage;
    private boolean cancelled;

    public ModelDamagedEvent(@NotNull HitBox hitBox, @NotNull ModelDamageSource source, float damage) {
        super(hitBox.source());
        this.hitBox = hitBox;
        this.source = source;
        this.damage = damage;
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
