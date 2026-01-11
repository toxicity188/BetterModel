package kr.toxicity.model.api.event;

public interface CancellableEvent extends ModelEvent {

    boolean isCancelled();

    boolean setCancelled(boolean isCanceled);
}
