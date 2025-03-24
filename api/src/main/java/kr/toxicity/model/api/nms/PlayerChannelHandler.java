package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PlayerChannelHandler extends AutoCloseable {
    @NotNull Player player();
    boolean isSlim();
    void startTrack(@NotNull EntityTracker tracker);
    void endTrack(@NotNull EntityTracker tracker);
    void unregisterAll();
    void showPlayerLimb(boolean show);
    boolean showPlayerLimb();
}
