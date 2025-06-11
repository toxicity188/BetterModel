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
     * @param instance render instance
     * @param modifier modifier
     */
    @ApiStatus.Internal
    public PlayerTracker(@NotNull EntityTrackerRegistry registry, @NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        super(registry, instance, modifier);
        instance.spawnFilter(p -> {
            var handler = BetterModel.plugin().playerManager().player(p.getUniqueId());
            return handler != null && handler.showPlayerLimb();
        });
    }

    @Override
    public boolean isRunningSingleAnimation() {
        return false;
    }

    @NotNull
    @Override
    public Player sourceEntity() {
        return (Player) super.sourceEntity();
    }
}
