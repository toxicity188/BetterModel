package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.PlayerTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
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

/**
 * A blueprint renderer.
 */
@RequiredArgsConstructor
public final class BlueprintRenderer {
    @Getter
    private final ModelBlueprint parent;
    private final Map<String, RendererGroup> rendererGroupMap;
    private final Map<String, BlueprintAnimation> animationMap;

    /**
     * Gets all name of animation.
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
                instance(null, entity.getLocation()),
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
                instance(player, player.getLocation()),
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
        return new VoidTracker(uuid, instance(null, location), modifier, location);
    }

    private @NotNull RenderInstance instance(@Nullable Player player, @NotNull Location location) {
        return new RenderInstance(this, rendererGroupMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().create(player, location))), animationMap);
    }
}
