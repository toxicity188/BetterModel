/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.armor.PlayerArmor;
import kr.toxicity.model.api.bone.BoneRenderContext;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.entity.BasePlayer;
import kr.toxicity.model.api.manager.SkinManager;
import kr.toxicity.model.api.nms.Profiled;
import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.player.PlayerSkinParts;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

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
    static @NotNull RenderSource.Dummy of(@NotNull PlatformLocation location) {
        return new DelegatedDummy(location);
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
    static @NotNull RenderSource.Dummy of(@NotNull PlatformLocation location, @NotNull ModelProfile.Uncompleted profile) {
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
    static @NotNull RenderSource.Entity of(@NotNull BaseEntity entity, @NotNull ModelProfile.Uncompleted profile) {
        return entity instanceof BasePlayer player ? new ProfiledPlayer(player, profile) : new ProfiledEntity(entity, profile);
    }

    /**
     * Creates an entity render source for the given entity.
     *
     * @param entity the entity to attach the model to
     * @return a new entity render source
     * @since 1.15.2
     */
    @ApiStatus.Internal
    static @NotNull RenderSource.Entity of(@NotNull BaseEntity entity) {
        return entity instanceof BasePlayer player ? new DelegatedPlayer(player) : new DelegatedEntity(entity);
    }

    /**
     * Returns the location of this render source.
     *
     * @return the location
     * @since 1.15.2
     */
    @NotNull PlatformLocation location();

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
        @NotNull BaseEntity entity();

        @Override
        default @NotNull PlatformLocation location() {
            return entity().location();
        }

        @NotNull
        @Override
        default EntityTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity()).create(pipeline.name(), r -> new EntityTracker(r, pipeline, modifier, preUpdateConsumer));
        }

        /**
         * Gets or creates an entity tracker for this source.
         *
         * @param name the name of the tracker
         * @param function a function for the render pipeline
         * @param modifier the tracker modifier
         * @param preUpdateConsumer a consumer to run before updates
         * @return the entity tracker
         * @since 1.15.2
         */
        default @NotNull EntityTracker getOrCreate(@NotNull String name, @NotNull Function<RenderSource.Entity, RenderPipeline> function, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity()).getOrCreate(name, r -> new EntityTracker(r, function.apply(this), modifier, preUpdateConsumer));
        }
    }

    /**
     * Represents a render source attached to a player.
     *
     * @since 3.2.0
     */
    sealed interface Player extends Entity, Profiled {
        @Override
        @NotNull BasePlayer entity();

        @NotNull
        @Override
        default EntityTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity()).create(pipeline.name(), r -> new PlayerTracker(r, pipeline, modifier, preUpdateConsumer));
        }

        @Override
        @NotNull
        default EntityTracker getOrCreate(@NotNull String name, @NotNull Function<Entity, RenderPipeline> function, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
            return EntityTrackerRegistry.getOrCreate(entity()).getOrCreate(name, r -> new PlayerTracker(r, function.apply(this), modifier, preUpdateConsumer));
        }

        @Override
        default @NotNull ModelProfile profile() {
            return entity().profile();
        }

        @Override
        default @NotNull PlayerArmor armors() {
            return entity().armors();
        }

        @Override
        default @NotNull PlayerSkinParts skinParts() {
            return entity().skinParts();
        }
    }

    /**
     * Represents a render source at a fixed location, not attached to an entity.
     *
     * @since 1.15.2
     */
    sealed interface Dummy extends RenderSource<DummyTracker> {
        @NotNull
        @Override
        default DummyTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
            return new DummyTracker(location(), pipeline, modifier, preUpdateConsumer);
        }
    }


    /**
     * A basic implementation of {@link Dummy} with a location.
     *
     * @param location the location
     * @since 1.15.2
     */
    record DelegatedDummy(@NotNull PlatformLocation location) implements Dummy {
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
    record ProfiledDummy(@NotNull PlatformLocation location, @NotNull ModelProfile.Uncompleted profile) implements Dummy {
        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return BetterModel.platform().manager(SkinManager.class).complete(profile).thenApply(skin -> new BoneRenderContext(this, skin));
        }
    }

    /**
     * A basic implementation of {@link Entity} wrapping a {@link BaseEntity}.
     *
     * @param entity the entity
     * @since 1.15.2
     */
    record DelegatedEntity(@NotNull BaseEntity entity) implements Entity {

        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return CompletableFuture.completedFuture(fallbackContext());
        }
    }

    /**
     * A profiled implementation of {@link Entity} wrapping a {@link BaseEntity} and a model profile.
     *
     * @param entity the entity
     * @param profile the model profile
     * @since 1.15.2
     */
    record ProfiledEntity(@NotNull BaseEntity entity, @NotNull ModelProfile.Uncompleted profile) implements Entity {

        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return BetterModel.platform().manager(SkinManager.class).complete(profile).thenApply(skin -> new BoneRenderContext(this, skin));
        }
    }

    /**
     * A basic implementation of {@link Entity} wrapping a {@link BasePlayer}.
     *
     * @param entity the player entity
     * @since 1.15.2
     */
    record DelegatedPlayer(@NotNull BasePlayer entity) implements Player {

        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return BetterModel.platform().manager(SkinManager.class).complete(profile().asUncompleted()).thenApply(skin -> new BoneRenderContext(this, skin));
        }
    }

    /**
     * A profiled implementation of {@link Entity} wrapping a {@link BasePlayer} and a model profile.
     *
     * @param entity the player entity
     * @param externalProfile the external model profile
     * @since 1.15.2
     */
    record ProfiledPlayer(@NotNull BasePlayer entity, @NotNull ModelProfile.Uncompleted externalProfile) implements Player {

        @Override
        public @NotNull CompletableFuture<BoneRenderContext> completeContext() {
            return BetterModel.platform().manager(SkinManager.class).complete(externalProfile).thenApply(skin -> new BoneRenderContext(this, skin));
        }
    }
}
