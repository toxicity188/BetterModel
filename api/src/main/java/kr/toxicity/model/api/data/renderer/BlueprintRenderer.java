package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.PlayerTracker;
import kr.toxicity.model.api.tracker.VoidTracker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class BlueprintRenderer {
    @Getter
    private final ModelBlueprint parent;
    private final Map<String, RendererGroup> rendererGroupMap;
    private final Map<String, BlueprintAnimation> animationMap;

    public @NotNull @Unmodifiable Set<String> animations() {
        return Collections.unmodifiableSet(animationMap.keySet());
    }

    public @NotNull String name() {
        return parent.name();
    }

    public @NotNull EntityTracker create(@NotNull Entity entity) {
        var tracker = EntityTracker.tracker(entity.getUniqueId());
        if (tracker != null) return tracker;
        if (entity instanceof Player player) {
            return new PlayerTracker(
                    player,
                    instance(player, entity.getLocation())
            );
        } else {
            return new EntityTracker(
                    entity,
                    instance(null, entity.getLocation())
            );
        }
    }

    public @NotNull VoidTracker create(@NotNull UUID uuid,  @NotNull Location location) {
        return new VoidTracker(uuid, instance(null, location), location);
    }

    private @NotNull RenderInstance instance(@Nullable Player player, @NotNull Location location) {
        return new RenderInstance(this, rendererGroupMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().create(player, location))), animationMap);
    }
}
