package kr.toxicity.model.api.event;

import kr.toxicity.model.api.BetterModel;
import org.bukkit.event.Event;

public abstract class AbstractModelEvent extends Event {
    public AbstractModelEvent() {
        super(!BetterModel.inst().nms().isSync());
    }
}
