/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.armor.PlayerArmor;
import kr.toxicity.model.api.bone.BoneRenderContext;
import kr.toxicity.model.api.nms.Profiled;
import kr.toxicity.model.api.player.PlayerSkinParts;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public sealed interface RenderSource<T extends Tracker> {

    @ApiStatus.Internal
    static @NotNull RenderSource.Dummy of(@NotNull Location location) {
        return new BaseDummy(location);
    }

    @ApiStatus.Internal
    static @NotNull RenderSource.Dummy of(@NotNull Location location, @NotNull ModelProfile.Uncompleted profile) {
        return new ProfiledDummy(location, profile);
    }

    @ApiStatus.Internal
    static @NotNull RenderSource.Entity of(@NotNull kr.toxicity.model.api.entity.BaseEntity entity, @NotNull ModelProfile.Uncompleted profile) {
        return entity instanceof kr.toxicity.model.api.entity.BasePlayer player ? new ProfiledPlayer(player, profile) : new ProfiledEntity(entity, profile);
    }

    @ApiStatus.Internal
    static @NotNull RenderSource.Entity of(@NotNull kr.toxicity.model.api.entity.BaseEntity entity) {
        return entity instanceof kr.toxicity.model.api.entity.BasePlayer player ? new BasePlayer(player) : new BaseEntity(entity);
    }

    @NotNull Location location();

    T create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<T> preUpdateConsumer);

    @NotNull CompletableFuture<BoneRenderContext> completeContext();

    default BoneRenderContext fallbackContext() {
        return new BoneRenderContext(this);
    }

    sealed interface Entity extends RenderSource<EntityTracker> {
        @NotNull kr.toxicity.model.api.entity.BaseEntity entity();
        @NotNull
        EntityTracker getOrCreate(@NotNull String name, @NotNull Supplier<RenderPipeline> supplier, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer);
    }

    sealed interface Dummy extends RenderSource<DummyTracker> {
    }


    record BaseDummy(@NotNull Location location) implements Dummy {
        @NotNull
        @Override
        public DummyTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
            return new DummyTracker(location, pipeline, modifier, preUpdateConsumer);
        }

        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return CompletableFuture.completedFuture(fallbackContext());
        }
    }

    record ProfiledDummy(@NotNull Location location, @NotNull ModelProfile.Uncompleted profile) implements Dummy {
        @NotNull
        @Override
        public DummyTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
            return new DummyTracker(location, pipeline, modifier, preUpdateConsumer);
        }

        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return BetterModel.plugin().skinManager().complete(profile).thenApply(skin -> new BoneRenderContext(this, skin));
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


        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return CompletableFuture.completedFuture(fallbackContext());
        }
    }

    record ProfiledEntity(@NotNull kr.toxicity.model.api.entity.BaseEntity entity, @NotNull ModelProfile.Uncompleted profile) implements Entity {

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

        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return BetterModel.plugin().skinManager().complete(profile).thenApply(skin -> new BoneRenderContext(this, skin));
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

        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return BetterModel.plugin().skinManager().complete(profile().asUncompleted()).thenApply(skin -> new BoneRenderContext(this, skin));
        }

        @Override
        public @NotNull ModelProfile profile() {
            return entity.profile();
        }

        @Override
        public @NotNull PlayerArmor armors() {
            return entity.armors();
        }

        @Override
        public @NotNull PlayerSkinParts skinParts() {
            return entity.skinParts();
        }
    }

    record ProfiledPlayer(@NotNull kr.toxicity.model.api.entity.BasePlayer entity, @NotNull ModelProfile.Uncompleted externalProfile) implements Entity, Profiled {
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

        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return BetterModel.plugin().skinManager().complete(externalProfile).thenApply(skin -> new BoneRenderContext(this, skin));
        }

        @Override
        public @NotNull ModelProfile profile() {
            return entity.profile();
        }

        @Override
        public @NotNull PlayerArmor armors() {
            return entity.armors();
        }

        @Override
        public @NotNull PlayerSkinParts skinParts() {
            return entity.skinParts();
        }
    }
}
