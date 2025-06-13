package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderPipeline;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Player tracker
 */
public final class PlayerTracker extends EntityTracker {

    /**
     * Creates player tracker
     * @param pipeline render instance
     * @param modifier modifier
     */
    @ApiStatus.Internal
    public PlayerTracker(@NotNull EntityTrackerRegistry registry, @NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier) {
        super(registry, pipeline, modifier);
        pipeline.spawnFilter(p -> {
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
