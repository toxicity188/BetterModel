package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.entity.EntityMovement;
import kr.toxicity.model.api.tracker.VoidTracker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BlueprintRenderer {
    @Getter
    private final float scale;
    private final Map<String, RendererGroup> rendererGroupMap;
    private final Map<String, BlueprintAnimation> animationMap;

    public static final EntityMovement DEFAULT = new EntityMovement(
            new Vector3f(),
            new Vector3f(1),
            new Quaternionf(),
            new Vector3f()
    );

    public @NotNull VoidTracker create(@NotNull Location location) {
        return new VoidTracker(
                new RenderInstance(rendererGroupMap
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                            var create = e.getValue().create(this, location);
                            create.move(DEFAULT);
                            return create;
                        })), animationMap),
                location
        );
    }
}
