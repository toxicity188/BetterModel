package kr.toxicity.model.api.event;

import kr.toxicity.model.api.BetterModel;
import org.bukkit.event.Event;

public abstract class AbstractModelEvent extends Event {
    public AbstractModelEvent() {
        this(!BetterModel.inst().nms().isSync());
    }
    public AbstractModelEvent(boolean async) {
        super(async);
    }
}
