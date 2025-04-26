package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Player tracker
 */
public final class PlayerTracker extends EntityTracker {

    /**
     * Creates player tracker
     * @param entity player
     * @param instance render instance
     * @param modifier modifier
     */
    @ApiStatus.Internal
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

    @NotNull
    @Override
    public Player source() {
        return (Player) super.source();
    }
}
