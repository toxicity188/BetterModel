package kr.toxicity.model.api.scheduler;

public interface ModelTask {
    boolean isCancelled();
    void cancel();
}
