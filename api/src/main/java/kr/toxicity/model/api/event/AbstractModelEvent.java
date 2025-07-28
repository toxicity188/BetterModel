package kr.toxicity.model.api.event;

import kr.toxicity.model.api.util.ThreadUtil;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;

/**
 * Abstract event of BetterModel
 */
public abstract class AbstractModelEvent extends Event {
    /**
     * Auto sync
     */
    @ApiStatus.Internal
    public AbstractModelEvent() {
        this(!ThreadUtil.isTickThread());
    }

    /**
     * Marked sync
     * @param async async
     */
    @ApiStatus.Internal
    public AbstractModelEvent(boolean async) {
        super(async);
    }
}
