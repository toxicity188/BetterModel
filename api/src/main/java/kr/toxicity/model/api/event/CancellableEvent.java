package kr.toxicity.model.api.event;

public interface CancellableEvent extends ModelEvent {

    boolean isCancelled();

    void setCancelled(boolean cancel);
}
