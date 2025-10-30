/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.renderer;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public sealed interface RenderSource<T extends Tracker> {

    @ApiStatus.Internal
    static @NotNull RenderSource.Dummy of(@NotNull Location location) {
        return new BaseDummy(location);
    }

    @ApiStatus.Internal
    static @NotNull RenderSource.Dummy of(@NotNull Location location, @NotNull GameProfile profile, boolean slim) {
        return new ProfiledDummy(location, profile, slim);
    }

    @ApiStatus.Internal
    static @NotNull RenderSource.Entity of(@NotNull kr.toxicity.model.api.entity.BaseEntity entity, @NotNull GameProfile profile, boolean slim) {
        return entity instanceof kr.toxicity.model.api.entity.BasePlayer player ? new ProfiledPlayer(player, profile, slim) : new ProfiledEntity(entity, profile, slim);
    }

    @ApiStatus.Internal
    static @NotNull RenderSource.Entity of(@NotNull kr.toxicity.model.api.entity.BaseEntity entity) {
        return entity instanceof kr.toxicity.model.api.entity.BasePlayer player ? new BasePlayer(player) : new BaseEntity(entity);
    }

    @NotNull Location location();
    T create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<T> preUpdateConsumer);

    sealed interface Entity extends RenderSource<EntityTracker> {
        @NotNull kr.toxicity.model.api.entity.BaseEntity entity();
        @NotNull
        EntityTracker getOrCreate(@NotNull String name, @NotNull Supplier<RenderPipeline> supplier, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer);
    }

    sealed interface Dummy extends RenderSource<DummyTracker> {
    }

    sealed interface Profiled {
        @NotNull GameProfile profile();
        boolean slim();
    }

    record BaseDummy(@NotNull Location location) implements Dummy {
        @NotNull
        @Override
        public DummyTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
            return new DummyTracker(location, pipeline, modifier, preUpdateConsumer);
        }
    }

    record ProfiledDummy(@NotNull Location location, @NotNull GameProfile profile, boolean slim) implements Profiled, Dummy {
        @NotNull
        @Override
        public DummyTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
            return new DummyTracker(location, pipeline, modifier, preUpdateConsumer);
        }
    }

    record BaseEntity(@NotNull kr.toxicity.model.api.entity.BaseEntity entity) implements Entity {

        @NotNull
        @Override
        public EntityTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity).create(pipeline.name(), r -> new EntityTracker(r, pipeline, modifier, preUpdateConsumer));
        }

        @Override
        public @NotNull EntityTracker getOrCreate(@NotNull String name, @NotNull Supplier<RenderPipeline> supplier, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity).getOrCreate(name, r -> new EntityTracker(r, supplier.get(), modifier, preUpdateConsumer));
        }

        @Override
        public @NotNull Location location() {
            return entity.location();
        }
    }

    record ProfiledEntity(@NotNull kr.toxicity.model.api.entity.BaseEntity entity, @NotNull GameProfile profile, boolean slim) implements Entity, Profiled {

        @NotNull
        @Override
        public EntityTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity).create(pipeline.name(), r -> new EntityTracker(r, pipeline, modifier, preUpdateConsumer));
        }

        @Override
        public @NotNull EntityTracker getOrCreate(@NotNull String name, @NotNull Supplier<RenderPipeline> supplier, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity).getOrCreate(name, r -> new EntityTracker(r, supplier.get(), modifier, preUpdateConsumer));
        }

        @Override
        public @NotNull Location location() {
            return entity.location();
        }
    }

    record BasePlayer(@NotNull kr.toxicity.model.api.entity.BasePlayer entity) implements Entity, Profiled {

        @NotNull
        @Override
        public EntityTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity).create(pipeline.name(), r -> new PlayerTracker(r, pipeline, modifier, preUpdateConsumer));
        }

        @Override
        public @NotNull EntityTracker getOrCreate(@NotNull String name, @NotNull Supplier<RenderPipeline> supplier, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity).getOrCreate(name, r -> new PlayerTracker(r, supplier.get(), modifier, preUpdateConsumer));
        }

        @Override
        public @NotNull Location location() {
            return entity.location();
        }

        @NotNull
        @Override
        public GameProfile profile() {
            return entity.profile();
        }

        @Override
        public boolean slim() {
            var channel = BetterModel.plugin().playerManager().player(entity.uuid());
            return channel != null && channel.isSlim();
        }
    }

    record ProfiledPlayer(@NotNull kr.toxicity.model.api.entity.BasePlayer entity, @NotNull GameProfile profile, boolean slim) implements Entity, Profiled {
        @NotNull
        @Override
        public EntityTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity).create(pipeline.name(), r -> new PlayerTracker(r, pipeline, modifier, preUpdateConsumer));
        }

        @Override
        public @NotNull EntityTracker getOrCreate(@NotNull String name, @NotNull Supplier<RenderPipeline> supplier, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity).getOrCreate(name, r -> new PlayerTracker(r, supplier.get(), modifier, preUpdateConsumer));
        }

        @Override
        public @NotNull Location location() {
            return entity.location();
        }
    }
}
