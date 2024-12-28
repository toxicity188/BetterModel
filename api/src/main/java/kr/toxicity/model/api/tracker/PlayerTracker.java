package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PlayerTracker extends EntityTracker {

    public PlayerTracker(@NotNull Player entity, @NotNull RenderInstance instance) {
        super(entity, instance);
        instance.filter(p -> BetterModel.inst().playerManager().player(p).showPlayerLimb());
    }

    @Override
    public boolean isRunningSingleAnimation() {
        return false;
    }
}
