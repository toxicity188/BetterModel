/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Manages all loaded models and provides access to them.
 * <p>
 * This manager is the primary entry point for retrieving {@link ModelRenderer} instances,
 * which are used to create and control model trackers.
 * </p>
 *
 * @since 1.15.2
 */
public interface ModelManager {

    /**
     * Retrieves a model renderer by its name.
     *
     * @param name the name of the model
     * @return the model renderer, or null if not found
     * @since 1.15.2
     */
    @Nullable ModelRenderer model(@NotNull String name);


    /**
     * @deprecated Use {@link #model(String)} instead.
     * @param name the name of the model
     * @return the model renderer, or null if not found
     * @since 1.15.2
     */
    @Deprecated
    @Nullable
    default ModelRenderer renderer(@NotNull String name) {
        return model(name);
    }

    /**
     * Returns a collection of all loaded model renderers.
     *
     * @return an unmodifiable collection of models
     * @since 1.15.2
     */
    @NotNull @Unmodifiable
    Collection<ModelRenderer> models();

    /**
     * Returns a set of all loaded model names.
     *
     * @return an unmodifiable set of model keys
     * @since 1.15.2
     */
    @NotNull @Unmodifiable
    Set<String> modelKeys();

    /**
     * Returns a collection of all renderers designated for player limb animations.
     *
     * @return an unmodifiable collection of limb models
     * @since 1.15.2
     */
    @NotNull @Unmodifiable
    Collection<ModelRenderer> limbs();

    /**
     * Retrieves a player limb renderer by its name.
     *
     * @param name the name of the limb model
     * @return the limb renderer, or null if not found
     * @since 1.15.2
     */
    @Nullable ModelRenderer limb(@NotNull String name);

    /**
     * Returns a set of all loaded player limb model names.
     *
     * @return an unmodifiable set of limb keys
     * @since 1.15.2
     */
    @NotNull @Unmodifiable
    Set<String> limbKeys();


    /**
     * Plays an animation on a player.
     *
     * @param player the target player
     * @param model the name of the limb model
     * @param animation the name of the animation
     * @return true if the animation started successfully
     * @since 1.15.2
     */
    default boolean animate(@NotNull Player player, @NotNull String model, @NotNull String animation) {
        return animate(player, model, animation, AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
    }

    /**
     * Plays an animation on a player with a specific modifier.
     *
     * @param player the target player
     * @param model the name of the limb model
     * @param animation the name of the animation
     * @param modifier the animation modifier
     * @return true if the animation started successfully
     * @since 1.15.2
     */
    default boolean animate(@NotNull Player player, @NotNull String model, @NotNull String animation, @NotNull AnimationModifier modifier) {
        return animate(player, model, animation, modifier, t -> {});
    }

    /**
     * Plays an animation on a player with a modifier and a configuration consumer.
     *
     * @param player the target player
     * @param model the name of the limb model
     * @param animation the name of the animation
     * @param modifier the animation modifier
     * @param consumer a consumer to configure the created tracker
     * @return true if the animation started successfully
     * @since 1.15.2
     */
    default boolean animate(@NotNull Player player, @NotNull String model, @NotNull String animation, @NotNull AnimationModifier modifier, @NotNull Consumer<EntityTracker> consumer) {
        var get = limb(model);
        if (get == null) return false;
        var create = get.getOrCreate(player, TrackerModifier.DEFAULT, consumer);
        if (!create.animate(animation, modifier, create::close)) {
            create.close();
            return false;
        }
        return true;
    }
}
