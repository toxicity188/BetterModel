package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.entity.EntityMovement;
import kr.toxicity.model.api.entity.RenderedEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class RenderInstance implements AutoCloseable {
    private final Map<String, RenderedEntity> entityMap;
    private final Map<String, BlueprintAnimation> animationMap;

    public RenderInstance(@NotNull Map<String, RenderedEntity> entityMap, @NotNull Map<String, BlueprintAnimation> animationMap) {
        this.entityMap = entityMap;
        this.animationMap = animationMap;

        animateLoop("idle");
    }

    @Override
    public void close() {
        entityMap.values().forEach(RenderedEntity::remove);
    }

    public void teleport(@NotNull Location location) {
        entityMap.values().forEach(e -> e.teleport(location));
    }

    public void move(@NotNull EntityMovement movement) {
        entityMap.values().forEach(e -> e.move(movement));
    }

    public boolean animateLoop(@NotNull String animation) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        for (RenderedEntity value : entityMap.values()) {
            value.addLoop(animation, get);
        }
        return true;
    }

    public void spawn(@NotNull Player player) {
        entityMap.values().forEach(e -> e.spawn(player));
    }
}
