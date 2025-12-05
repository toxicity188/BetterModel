/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.mapValue;

/**
 * A blueprint renderer.
 *
 * @param name name
 * @param type type
 * @param rendererGroups group map
 * @param animations animations
 */
public record ModelRenderer(
    @NotNull String name,
    @NotNull Type type,
    @NotNull @Unmodifiable Map<BoneName, RendererGroup> rendererGroups,
    @NotNull @Unmodifiable Map<String, BlueprintAnimation> animations
) {
    /**
     * Gets a renderer group by tree
     *
     * @param name part name
     * @return group or null
     */
    public @Nullable RendererGroup groupByTree(@NotNull BoneName name) {
        return groupByTree0(rendererGroups, name);
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
        return rendererGroups.values().stream().flatMap(RendererGroup::flatten);
    }

    /**
     * Gets blueprint animation by name
     *
     * @param name name
     * @return optional animation
     */
    public @NotNull Optional<BlueprintAnimation> animation(@NotNull String name) {
        return Optional.ofNullable(animations().get(name));
    }

    /**
     * Gets model's name.
     *
     * @return name
     */
    public @NotNull String name() {
        return name;
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
            pipeline(source),
            modifier,
            preUpdateConsumer
        );
    }

    /**
     * Creates tracker by location and completed profile
     *
     * @param location location
     * @param profile  profile
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull ModelProfile profile) {
        return create(location, profile.asUncompleted());
    }

    /**
     * Creates tracker by location and completed profile
     *
     * @param location location
     * @param profile  profile
     * @param modifier modifier
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, ModelProfile profile, @NotNull TrackerModifier modifier) {
        return create(location, profile.asUncompleted(), modifier);
    }

    /**
     * Creates tracker by location and completed profile
     *
     * @param location          location
     * @param profile           profile
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull ModelProfile profile, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
        return create(location, profile.asUncompleted(), modifier, preUpdateConsumer);
    }

    /**
     * Creates tracker by location and uncompleted profile
     *
     * @param location location
     * @param profile  profile
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull ModelProfile.Uncompleted profile) {
        return create(location, profile, TrackerModifier.DEFAULT);
    }

    /**
     * Creates tracker by location and uncompleted profile
     *
     * @param location location
     * @param profile  profile
     * @param modifier modifier
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, ModelProfile.Uncompleted profile, @NotNull TrackerModifier modifier) {
        return create(location, profile, modifier, t -> {
        });
    }

    /**
     * Creates tracker by location and uncompleted profile
     *
     * @param location          location
     * @param profile           profile
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return empty tracker
     */
    public @NotNull DummyTracker create(@NotNull Location location, @NotNull ModelProfile.Uncompleted profile, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
        var source = RenderSource.of(location, profile);
        return source.create(
            pipeline(source),
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
        return create(BaseEntity.of(entity));
    }

    /**
     * Creates tracker by entity
     *
     * @param entity   entity
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull TrackerModifier modifier) {
        return create(BaseEntity.of(entity), modifier);
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
        return create(BaseEntity.of(entity), modifier, preUpdateConsumer);
    }

    /**
     * Creates tracker by entity and uncompleted profile
     *
     * @param entity   entity
     * @param profile  profile
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull ModelProfile.Uncompleted profile) {
        return create(BaseEntity.of(entity), profile);
    }

    /**
     * Creates tracker by entity and uncompleted profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull ModelProfile.Uncompleted profile, @NotNull TrackerModifier modifier) {
        return create(BaseEntity.of(entity), profile, modifier);
    }

    /**
     * Creates tracker by entity and uncompleted profile
     *
     * @param entity            entity
     * @param profile           skin
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull Entity entity, @NotNull ModelProfile.Uncompleted profile, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        return create(BaseEntity.of(entity), profile, modifier, preUpdateConsumer);
    }

    /**
     * Gets or creates tracker by entity
     *
     * @param entity entity
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity) {
        return getOrCreate(BaseEntity.of(entity));
    }

    /**
     * Gets or creates tracker by entity
     *
     * @param entity   entity
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull TrackerModifier modifier) {
        return getOrCreate(BaseEntity.of(entity), modifier);
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
        return getOrCreate(BaseEntity.of(entity), modifier, preUpdateConsumer);
    }

    /**
     * Gets or creates tracker by entity and completed profile
     *
     * @param entity   entity
     * @param profile  profile
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull ModelProfile profile) {
        return getOrCreate(entity, profile.asUncompleted());
    }

    /**
     * Gets or creates tracker by entity and completed profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull ModelProfile profile, @NotNull TrackerModifier modifier) {
        return getOrCreate(entity, profile.asUncompleted(), modifier);
    }

    /**
     * Gets or creates tracker by entity and completed profile
     *
     * @param entity            entity
     * @param profile           skin
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull ModelProfile profile, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        return getOrCreate(entity, profile.asUncompleted(), modifier, preUpdateConsumer);
    }

    /**
     * Gets or creates tracker by entity and uncompleted profile
     *
     * @param entity   entity
     * @param profile  profile
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull ModelProfile.Uncompleted profile) {
        return getOrCreate(BaseEntity.of(entity), profile);
    }

    /**
     * Gets or creates tracker by entity and uncompleted profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull ModelProfile.Uncompleted profile, @NotNull TrackerModifier modifier) {
        return getOrCreate(BaseEntity.of(entity), profile, modifier);
    }

    /**
     * Gets or creates tracker by entity and uncompleted profile
     *
     * @param entity            entity
     * @param profile           skin
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull Entity entity, @NotNull ModelProfile.Uncompleted profile, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        return getOrCreate(BaseEntity.of(entity), profile, modifier, preUpdateConsumer);
    }

    /**
     * Creates tracker by entity
     *
     * @param entity entity
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull BaseEntity entity) {
        return create(entity, TrackerModifier.DEFAULT);
    }
    /**
     * Creates tracker by entity
     *
     * @param entity   entity
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull BaseEntity entity, @NotNull TrackerModifier modifier) {
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
    public @NotNull EntityTracker create(@NotNull BaseEntity entity, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        var source = RenderSource.of(entity);
        return source.create(
            pipeline(source),
            modifier,
            preUpdateConsumer
        );
    }

    /**
     * Creates tracker by entity and completed profile
     *
     * @param entity   entity
     * @param profile  profile
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull BaseEntity entity, @NotNull ModelProfile profile) {
        return create(entity, profile.asUncompleted());
    }

    /**
     * Creates tracker by entity and completed profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull BaseEntity entity, @NotNull ModelProfile profile, @NotNull TrackerModifier modifier) {
        return create(entity, profile.asUncompleted(), modifier);
    }

    /**
     * Creates tracker by entity and completed profile
     *
     * @param entity            entity
     * @param profile           profile
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull BaseEntity entity, @NotNull ModelProfile profile, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        return create(entity, profile.asUncompleted(), modifier, preUpdateConsumer);
    }

    /**
     * Creates tracker by entity and uncompleted profile
     *
     * @param entity   entity
     * @param profile  profile
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull BaseEntity entity, @NotNull ModelProfile.Uncompleted profile) {
        return create(entity, profile, TrackerModifier.DEFAULT);
    }

    /**
     * Creates tracker by entity and uncompleted profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull BaseEntity entity, @NotNull ModelProfile.Uncompleted profile, @NotNull TrackerModifier modifier) {
        return create(entity, profile, modifier, t -> {
        });
    }

    /**
     * Creates tracker by entity and uncompleted profile
     *
     * @param entity            entity
     * @param profile           profile
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker create(@NotNull BaseEntity entity, @NotNull ModelProfile.Uncompleted profile, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        var source = RenderSource.of(entity, profile);
        return source.create(
            pipeline(source),
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
    public @NotNull EntityTracker getOrCreate(@NotNull BaseEntity entity) {
        return getOrCreate(entity, TrackerModifier.DEFAULT);
    }

    /**
     * Gets or creates tracker by entity
     *
     * @param entity   entity
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull BaseEntity entity, @NotNull TrackerModifier modifier) {
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
    public @NotNull EntityTracker getOrCreate(@NotNull BaseEntity entity, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        var source = RenderSource.of(entity);
        return source.getOrCreate(
            name(),
            () -> pipeline(source),
            modifier,
            preUpdateConsumer
        );
    }

    /**
     * Gets or creates tracker by entity and completed profile
     *
     * @param entity   entity
     * @param profile  profile
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull BaseEntity entity, @NotNull ModelProfile profile) {
        return getOrCreate(entity, profile.asUncompleted());
    }


    /**
     * Gets or creates tracker by entity and completed profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull BaseEntity entity, ModelProfile profile, @NotNull TrackerModifier modifier) {
        return getOrCreate(entity, profile.asUncompleted(), modifier);
    }

    /**
     * Gets or creates tracker by entity and completed profile
     *
     * @param entity            entity
     * @param profile           profile
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull BaseEntity entity, @NotNull ModelProfile profile, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        return getOrCreate(entity, profile.asUncompleted(), modifier, preUpdateConsumer);
    }

    /**
     * Gets or creates tracker by entity and uncompleted profile
     *
     * @param entity   entity
     * @param profile  profile
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull BaseEntity entity, @NotNull ModelProfile.Uncompleted profile) {
        return getOrCreate(entity, profile, TrackerModifier.DEFAULT);
    }


    /**
     * Gets or creates tracker by entity and uncompleted profile
     *
     * @param entity   entity
     * @param profile  profile
     * @param modifier modifier
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull BaseEntity entity, ModelProfile.Uncompleted profile, @NotNull TrackerModifier modifier) {
        return getOrCreate(entity, profile, modifier, t -> {
        });
    }

    /**
     * Gets or creates tracker by entity and uncompleted profile
     *
     * @param entity            entity
     * @param profile           profile
     * @param modifier          modifier
     * @param preUpdateConsumer task on pre-update
     * @return entity tracker
     */
    public @NotNull EntityTracker getOrCreate(@NotNull BaseEntity entity, @NotNull ModelProfile.Uncompleted profile, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        var source = RenderSource.of(entity, profile);
        return source.getOrCreate(
            name(),
            () -> pipeline(source),
            modifier,
            preUpdateConsumer
        );
    }

    private @NotNull RenderPipeline pipeline(@NotNull RenderSource<?> source) {
        return new RenderPipeline(this, source, mapValue(rendererGroups, value -> value.create(source)));
    }

    /**
     * Renderer type
     */
    @RequiredArgsConstructor
    @Getter
    public enum Type {
        /**
         * General
         */
        GENERAL(true),
        /**
         * Player
         */
        PLAYER(false)
        ;
        private final boolean canBeSaved;
    }
}
