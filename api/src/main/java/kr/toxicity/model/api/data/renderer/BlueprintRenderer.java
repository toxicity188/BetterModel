package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.VoidTracker;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class BlueprintRenderer {
    private final Map<String, RendererGroup> rendererGroupMap;
    private final Map<String, BlueprintAnimation> animationMap;

    public @NotNull EntityTracker create(@NotNull Entity entity) {
        return new EntityTracker(
                entity,
                instance(entity.getLocation())
        );
    }

    public @NotNull VoidTracker create(@NotNull UUID uuid,  @NotNull Location location) {
        return new VoidTracker(uuid, instance(location), location);
    }

    private @NotNull RenderInstance instance(@NotNull Location location) {
        return new RenderInstance(rendererGroupMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().create(location))), animationMap);
    }
}
