package kr.toxicity.model.api.data.renderer;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.mapValue;

/**
 * A blueprint renderer.
 *
 * @param parent parent blueprint
 * @param rendererGroupMap group map
 * @param animationMap animation map
 */
public record ModelRenderer(
        @NotNull ModelBlueprint parent,
        @NotNull @Unmodifiable Map<BoneName, RendererGroup> rendererGroupMap,
        @NotNull @Unmodifiable Map<String, BlueprintAnimation> animationMap
) {
    /**
     * Gets a renderer group by tree
     *
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
     * Gets flatten groups.
     * @return flatten groups
     */
    public @NotNull Stream<RendererGroup> flatten() {
        return rendererGroupMap.values().stream().flatMap(RendererGroup::flatten);
    }

    /**
     * Gets all names of animation.
     * @return names
     */
    public @NotNull @Unmodifiable Set<String> animations() {
        return animationMap.keySet();
    }

    /**
     * Gets blueprint animation by name
     *
     * @param name name
     * @return optional animation
     */
    public @NotNull Optional<BlueprintAnimation> animation(@NotNull String name) {
        return Optional.ofNullable(animationMap.get(name));
    }

    /**
     * Gets model's name.
     *
     * @return name
     */
    public @NotNull String name() {
        return parent.name();
    }

    //----- Dummy -----

    /**
     * Creates tracker by location
     *
     * @param location location
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location) {
        return create(location, TrackerModifier.DEFAULT);
    }

    /**
     * Creates tracker by location
     *
     * @param location location
     * @param modifier modifier
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull TrackerModifier modifier) {
        return create(location, modifier, t -> {
        });
    }

    /**
     * Creates tracker by location
     *
     * @param location          location
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
        var source = RenderSource.of(location);
        return source.create(
                pipeline(source, modifier),
                modifier,
                preUpdateConsumer
        );
    }

    /**
     * Creates tracker by location and player
     *
     * @param location location
     * @param player   player
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull OfflinePlayer player) {
        return create(location, player, TrackerModifier.DEFAULT);
    }

    /**
     * Creates tracker by location and profile
     *
     * @param location location
     * @param profile  profile
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull GameProfile profile) {
        return create(location, profile, TrackerModifier.DEFAULT);
    }

    /**
     * Creates tracker by location and profile
     *
     * @param location location
     * @param profile  profile
     * @param slim     slim
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull GameProfile profile, boolean slim) {
        return create(location, profile, slim, TrackerModifier.DEFAULT);
    }

    /**
     * Creates tracker by location and player
     *
     * @param location location
     * @param player   player
     * @param modifier modifier
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull OfflinePlayer player, @NotNull TrackerModifier modifier) {
        var channel = BetterModel.plugin().playerManager().player(player.getUniqueId());
        return channel == null ? create(location, BetterModel.plugin().nms().profile(player), modifier) : create(location, channel.profile(), channel.isSlim(), modifier);
    }

    /**
     * Creates tracker by location and profile
     *
     * @param location location
     * @param profile  profile
     * @param modifier modifier
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull GameProfile profile, @NotNull TrackerModifier modifier) {
        return create(location, profile, BetterModel.plugin().skinManager().isSlim(profile), modifier);
    }

    /**
     * Creates tracker by location and profile
     *
     * @param location location
     * @param profile  profile
     * @param slim     slim
     * @param modifier modifier
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull GameProfile profile, boolean slim, @NotNull TrackerModifier modifier) {
        return create(location, profile, slim, modifier, t -> {
        });
    }

    /**
     * Creates tracker by location and profile
     *
     * @param location          location
     * @param profile           profile
     * @param slim              slim
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull GameProfile profile, boolean slim, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
        var source = RenderSource.of(location, profile, slim);
        return source.create(
                pipeline(source, modifier),
                modifier,
                preUpdateConsumer
        );
    }

    //----- Entity -----

    /**
     * Creates tracker by entity
     *
     * @param entity entity
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity) {
        return create(entity, TrackerModifier.DEFAULT);
    }

    /**
     * Creates tracker by entity and profile
     *
     * @param entity entity
     * @param player   player
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull OfflinePlayer player) {
        return create(entity, player, TrackerModifier.DEFAULT);
    }

    /**
     * Creates tracker by entity and player
     *
     * @param entity entity
     * @param player  player
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull OfflinePlayer player, @NotNull TrackerModifier modifier) {
        var channel = BetterModel.plugin().playerManager().player(player.getUniqueId());
        return channel == null ? create(entity, BetterModel.plugin().nms().profile(player), modifier) : create(entity, channel.profile(), channel.isSlim(), modifier);
    }

    /**
     * Creates tracker by entity
     *
     * @param entity   entity
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull TrackerModifier modifier) {
        return create(entity, modifier, t -> {
        });
    }

    /**
     * Creates tracker by entity
     *
     * @param entity            entity
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        var source = RenderSource.of(entity);
        return source.create(
                pipeline(source, modifier),
                modifier,
                preUpdateConsumer
        );
    }

    /**
     * Creates tracker by entity and profile
     *
     * @param entity   entity
     * @param profile  profile
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull GameProfile profile) {
        return create(entity, profile, BetterModel.plugin().skinManager().isSlim(profile));
    }

    /**
     * Creates tracker by entity and profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param slim     slim
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull GameProfile profile, boolean slim) {
        return create(entity, profile, slim, TrackerModifier.DEFAULT);
    }

    /**
     * Creates tracker by entity and profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull GameProfile profile, @NotNull TrackerModifier modifier) {
        return create(entity, profile, BetterModel.plugin().skinManager().isSlim(profile), modifier);
    }

    /**
     * Creates tracker by entity and profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param slim     slim
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull GameProfile profile, boolean slim, @NotNull TrackerModifier modifier) {
        return create(entity, profile, slim, modifier, t -> {
        });
    }

    /**
     * Creates tracker by entity and profile
     *
     * @param entity            entity
     * @param profile           profile
     * @param slim              slim
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull GameProfile profile, boolean slim, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        var source = RenderSource.of(entity, profile, slim);
        return source.create(
                pipeline(source, modifier),
                modifier,
                preUpdateConsumer
        );
    }

    /**
     * Gets or creates tracker by entity
     *
     * @param entity entity
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity) {
        return getOrCreate(entity, TrackerModifier.DEFAULT);
    }

    /**
     * Gets or creates tracker by entity
     *
     * @param entity   entity
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull TrackerModifier modifier) {
        return getOrCreate(entity, modifier, t -> {
        });
    }

    /**
     * Gets or creates tracker by entity
     *
     * @param entity            entity
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        var source = RenderSource.of(entity);
        return source.getOrCreate(
                name(),
                () -> pipeline(source, modifier),
                modifier,
                preUpdateConsumer
        );
    }

    /**
     * Gets or creates tracker by entity and profile
     *
     * @param entity entity
     * @param player   player
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull OfflinePlayer player) {
        return getOrCreate(entity, player, TrackerModifier.DEFAULT);
    }

    /**
     * Gets or creates tracker by entity and player
     *
     * @param entity entity
     * @param player  player
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull OfflinePlayer player, @NotNull TrackerModifier modifier) {
        var channel = BetterModel.plugin().playerManager().player(player.getUniqueId());
        return channel == null ? getOrCreate(entity, BetterModel.plugin().nms().profile(player), modifier) : getOrCreate(entity, channel.profile(), channel.isSlim(), modifier);
    }

    /**
     * Gets or creates tracker by entity and profile
     *
     * @param entity   entity
     * @param profile  profile
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull GameProfile profile) {
        return getOrCreate(entity, profile, BetterModel.plugin().skinManager().isSlim(profile));
    }

    /**
     * Gets or creates tracker by entity and profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param slim     slim
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull GameProfile profile, boolean slim) {
        return getOrCreate(entity, profile, slim, TrackerModifier.DEFAULT);
    }

    /**
     * Gets or creates tracker by entity and profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull GameProfile profile, @NotNull TrackerModifier modifier) {
        return getOrCreate(entity, profile, BetterModel.plugin().skinManager().isSlim(profile), modifier);
    }

    /**
     * Gets or creates tracker by entity and profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param slim     slim
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull GameProfile profile, boolean slim, @NotNull TrackerModifier modifier) {
        return getOrCreate(entity, profile, slim, modifier, t -> {
        });
    }

    /**
     * Gets or creates tracker by entity and profile
     *
     * @param entity            entity
     * @param profile           profile
     * @param slim              slim
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull GameProfile profile, boolean slim, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        var source = RenderSource.of(entity, profile, slim);
        return source.getOrCreate(
                name(),
                () -> pipeline(source, modifier),
                modifier,
                preUpdateConsumer
        );
    }

    private @NotNull RenderPipeline pipeline(@NotNull RenderSource<?> source, @NotNull TrackerModifier modifier) {
        return new RenderPipeline(this, source, mapValue(rendererGroupMap, value -> value.create(source, modifier)));
    }
}
