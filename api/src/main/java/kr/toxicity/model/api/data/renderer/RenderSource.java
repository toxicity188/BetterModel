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

/**
 * Represents a source for rendering models, providing necessary context such as location and entity data.
 * <p>
 * This interface serves as the entry point for creating {@link Tracker} instances, which manage the lifecycle of a rendered model.
 * It supports both entity-based and location-based (dummy) rendering sources.
 * </p>
 *
 * @param <T> the type of tracker created by this source
 * @since 1.15.2
 */
public sealed interface RenderSource<T extends Tracker> {

    /**
     * Creates a dummy render source at the specified location.
     *
     * @param location the location where the model will be rendered
     * @return a new dummy render source
     * @since 1.15.2
     */
    @ApiStatus.Internal
    static @NotNull RenderSource.Dummy of(@NotNull Location location) {
        return new BaseDummy(location);
    }

    /**
     * Creates a dummy render source at the specified location with a specific model profile.
     *
     * @param location the location where the model will be rendered
     * @param profile the uncompleted model profile to use
     * @return a new profiled dummy render source
     * @since 1.15.2
     */
    @ApiStatus.Internal
    static @NotNull RenderSource.Dummy of(@NotNull Location location, @NotNull ModelProfile.Uncompleted profile) {
        return new ProfiledDummy(location, profile);
    }

    /**
     * Creates an entity render source for the given entity with a specific model profile.
     *
     * @param entity the entity to attach the model to
     * @param profile the uncompleted model profile to use
     * @return a new entity render source
     * @since 1.15.2
     */
    @ApiStatus.Internal
    static @NotNull RenderSource.Entity of(@NotNull kr.toxicity.model.api.entity.BaseEntity entity, @NotNull ModelProfile.Uncompleted profile) {
        return entity instanceof kr.toxicity.model.api.entity.BasePlayer player ? new ProfiledPlayer(player, profile) : new ProfiledEntity(entity, profile);
    }

    /**
     * Creates an entity render source for the given entity.
     *
     * @param entity the entity to attach the model to
     * @return a new entity render source
     * @since 1.15.2
     */
    @ApiStatus.Internal
    static @NotNull RenderSource.Entity of(@NotNull kr.toxicity.model.api.entity.BaseEntity entity) {
        return entity instanceof kr.toxicity.model.api.entity.BasePlayer player ? new BasePlayer(player) : new BaseEntity(entity);
    }

    /**
     * Returns the location of this render source.
     *
     * @return the location
     * @since 1.15.2
     */
    @NotNull Location location();

    /**
     * Creates a new tracker for this render source.
     *
     * @param pipeline the render pipeline to use
     * @param modifier the tracker modifier
     * @param preUpdateConsumer a consumer to run before updates
     * @return the created tracker
     * @since 1.15.2
     */
    T create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<T> preUpdateConsumer);

    /**
     * Asynchronously completes the bone render context for this source.
     * <p>
     * This method may involve fetching skin data or other resources.
     * </p>
     *
     * @return a future completing with the bone render context
     * @since 1.15.2
     */
    @NotNull CompletableFuture<BoneRenderContext> completeContext();

    /**
     * Returns a fallback bone render context.
     * <p>
     * This context is used when the complete context cannot be resolved or is not yet available.
     * </p>
     *
     * @return the fallback bone render context
     * @since 1.15.2
     */
    default BoneRenderContext fallbackContext() {
        return new BoneRenderContext(this);
    }

    /**
     * Represents a render source attached to an entity.
     *
     * @since 1.15.2
     */
    sealed interface Entity extends RenderSource<EntityTracker> {
        /**
         * Returns the entity associated with this source.
         *
         * @return the entity
         * @since 1.15.2
         */
        @NotNull kr.toxicity.model.api.entity.BaseEntity entity();

        /**
         * Gets or creates an entity tracker for this source.
         *
         * @param name the name of the tracker
         * @param supplier a supplier for the render pipeline
         * @param modifier the tracker modifier
         * @param preUpdateConsumer a consumer to run before updates
         * @return the entity tracker
         * @since 1.15.2
         */
        @NotNull
        EntityTracker getOrCreate(@NotNull String name, @NotNull Supplier<RenderPipeline> supplier, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer);
    }

    /**
     * Represents a render source at a fixed location, not attached to an entity.
     *
     * @since 1.15.2
     */
    sealed interface Dummy extends RenderSource<DummyTracker> {
    }


    /**
     * A basic implementation of {@link Dummy} with a location.
     *
     * @param location the location
     * @since 1.15.2
     */
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

    /**
     * A profiled implementation of {@link Dummy} with a location and a model profile.
     *
     * @param location the location
     * @param profile the model profile
     * @since 1.15.2
     */
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

    /**
     * A basic implementation of {@link Entity} wrapping a {@link kr.toxicity.model.api.entity.BaseEntity}.
     *
     * @param entity the entity
     * @since 1.15.2
     */
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

    /**
     * A profiled implementation of {@link Entity} wrapping a {@link kr.toxicity.model.api.entity.BaseEntity} and a model profile.
     *
     * @param entity the entity
     * @param profile the model profile
     * @since 1.15.2
     */
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

    /**
     * A basic implementation of {@link Entity} wrapping a {@link kr.toxicity.model.api.entity.BasePlayer}.
     *
     * @param entity the player entity
     * @since 1.15.2
     */
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

    /**
     * A profiled implementation of {@link Entity} wrapping a {@link kr.toxicity.model.api.entity.BasePlayer} and a model profile.
     *
     * @param entity the player entity
     * @param externalProfile the external model profile
     * @since 1.15.2
     */
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
