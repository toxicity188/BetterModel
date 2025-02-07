package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PlayerTracker extends EntityTracker {

    private final Player player;

    public PlayerTracker(@NotNull Player entity, @NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        super(entity, instance, modifier);
        this.player = entity;
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
        player.updateInventory();
    }
}
