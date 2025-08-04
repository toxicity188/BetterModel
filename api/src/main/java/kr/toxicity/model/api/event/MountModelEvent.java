package kr.toxicity.model.api.event;

import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.tracker.EntityTracker;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Mounts to some model's hitbox event
 */
@Getter
@Setter
public final class MountModelEvent extends AbstractModelEvent implements Cancellable {
    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final EntityTracker tracker;
    private final HitBox hitBox;
    private final Entity entity;
    private boolean cancelled;

    /**
     * Creates event
     * @param tracker tracker
     * @param hitBox hitbox
     * @param entity entity
     */
    @ApiStatus.Internal
    public MountModelEvent(@NotNull EntityTracker tracker, @NotNull HitBox hitBox, @NotNull Entity entity) {
        this.tracker = tracker;
        this.hitBox = hitBox;
        this.entity = entity;
    }

    /**
     * Gets entity tracker
     * @return entity tracker
     */
    public @NotNull EntityTracker tracker() {
        return tracker;
    }

    /**
     * Gets hitbox
     * @return hitbox
     */
    public @NotNull HitBox hitbox() {
        return hitBox;
    }

    /**
     * Gets passenger entity
     * @return entity
     */
    public Entity entity() {
        return entity;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
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
