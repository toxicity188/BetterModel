package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PlayerTracker extends EntityTracker {

    public PlayerTracker(@NotNull Player entity, @NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        super(entity, instance, modifier);
        instance.spawnFilter(p -> {
            var handler = BetterModel.inst().playerManager().player(p.getUniqueId());
            return handler != null && handler.showPlayerLimb();
        });
    }

    @Override
    public boolean isRunningSingleAnimation() {
        return false;
    }

    @Override
    public void close() throws Exception {
        super.close();
        source().updateInventory();
    }

    @NotNull
    @Override
    public Player source() {
        return (Player) super.source();
    }
}
