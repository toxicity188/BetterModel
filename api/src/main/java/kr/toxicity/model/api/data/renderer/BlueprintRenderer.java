package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import kr.toxicity.model.api.tracker.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A blueprint renderer.
 */
@RequiredArgsConstructor
public final class BlueprintRenderer {
    @Getter
    private final ModelBlueprint parent;
    @Getter
    @Unmodifiable
    private final Map<BoneName, RendererGroup> rendererGroupMap;
    private final Map<String, BlueprintAnimation> animationMap;

    /**
     * Gets a renderer group by tree
     * @param name part name
     * @return group or null
     */
    public @Nullable RendererGroup groupByTree(@NotNull BoneName name) {
        return groupByTree0(rendererGroupMap, name);
    }

    private static @Nullable RendererGroup groupByTree0(@NotNull Map<BoneName, RendererGroup> map, @NotNull BoneName name) {
        if (map.isEmpty()) return null;
        var get = map.get(name);
        if (get != null) return get;
        else return map.values()
                .stream()
                .map(g -> groupByTree0(g.getChildren(), name))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all names of animation.
     * @return names
     */
    public @NotNull @Unmodifiable Set<String> animations() {
        return Collections.unmodifiableSet(animationMap.keySet());
    }

    /**
     * Gets model's name.
     * @return name
     */
    public @NotNull String name() {
        return parent.name();
    }

    /**
     * Gets or creates tracker by entity.
     * @param entity target
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity) {
        return create(entity, TrackerModifier.DEFAULT);
    }
    /**
     * Gets or creates tracker by entity.
     * @param entity target
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull TrackerModifier modifier) {
        var tracker = EntityTracker.tracker(entity.getUniqueId());
        if (tracker != null) return tracker;
        return new EntityTracker(
                entity,
                instance(null, entity.getLocation().add(0, -1024, 0), modifier),
                modifier
        );
    }

    /**
     * Gets or creates tracker by player
     * @param player player
     * @return player limb tracker
     */
    public @NotNull EntityTracker createPlayerLimb(@NotNull Player player) {
        return createPlayerLimb(player, TrackerModifier.DEFAULT);
    }
    /**
     * Gets or creates tracker by player
     * @param player player
     * @param modifier modifier
     * @return player limb tracker
     */
    public @NotNull EntityTracker createPlayerLimb(@NotNull Player player, @NotNull TrackerModifier modifier) {
        var tracker = EntityTracker.tracker(player.getUniqueId());
        if (tracker != null) return tracker;
        return new PlayerTracker(
                player,
                instance(player, player.getLocation().add(0, -1024, 0), modifier),
                modifier
        );
    }

    /**
     * Creates tracker by location.
     * @param uuid uuid
     * @param location location
     * @return void tracker
     */
    public @NotNull VoidTracker create(@NotNull UUID uuid, @NotNull Location location) {
        return create(uuid, TrackerModifier.DEFAULT, location);
    }

    /**
     * Creates tracker by location.
     * @param uuid uuid
     * @param modifier modifier
     * @param location location
     * @return void tracker
     */
    public @NotNull VoidTracker create(@NotNull UUID uuid, @NotNull TrackerModifier modifier, @NotNull Location location) {
        return new VoidTracker(uuid, instance(null, location, modifier), modifier, location);
    }

    private @NotNull RenderInstance instance(@Nullable Player player, @NotNull Location location, @NotNull TrackerModifier modifier) {
        return new RenderInstance(this, rendererGroupMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().create(player, modifier, location))), animationMap);
    }
}
