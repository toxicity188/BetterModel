package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.data.renderer.RenderPipeline;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Player tracker
 */
public final class PlayerTracker extends EntityTracker {

    /**
     * Creates player tracker
     * @param registry registry
     * @param pipeline render instance
     * @param modifier modifier
     * @param preUpdateConsumer task on pre-update
     */
    @ApiStatus.Internal
    public PlayerTracker(@NotNull EntityTrackerRegistry registry, @NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        super(registry, pipeline, modifier, preUpdateConsumer);
        bodyRotator().setValue(setter -> setter.setPlayerMode(true));
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
