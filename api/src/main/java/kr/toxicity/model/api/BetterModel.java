/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api;

import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

import static kr.toxicity.model.api.util.ReflectionUtil.classExists;

/**
 * The main entry point for the BetterModel API.
 * <p>
 * This class provides static access to the plugin instance, configuration, model managers,
 * NMS handlers, and entity registries. It serves as a service provider for interacting with the BetterModel engine.
 * </p>
 *
 * @since 1.15.2
 */
public final class BetterModel {

    /**
     * Private initializer to prevent instantiation.
     */
    private BetterModel() {
        throw new RuntimeException();
    }

    /**
     * Checks if the server is running on the Folia platform.
     * @since 1.15.2
     */
    public static final boolean IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    /**
     * Checks if the server is running on the Purpur platform.
     * @since 1.15.2
     */
    public static final boolean IS_PURPUR = classExists("org.purpurmc.purpur.PurpurConfig");
    /**
     * Checks if the server is running on the Paper platform (or a fork like Purpur/Folia).
     * @since 1.15.2
     */
    public static final boolean IS_PAPER = IS_PURPUR || IS_FOLIA || classExists("io.papermc.paper.configuration.PaperConfigurations");

    /**
     * The singleton plugin instance.
     */
    private static BetterModelPlugin instance;

    /**
     * Returns the plugin configuration manager.
     *
     * @return the configuration manager
     * @since 1.15.2
     */
    public static @NotNull BetterModelConfig config() {
        return plugin().config();
    }

    /**
     * Retrieves a model renderer by its name, wrapped in an Optional.
     *
     * @param name the name of the model
     * @return an optional containing the renderer if found
     * @since 1.15.2
     */
    public static @NotNull Optional<ModelRenderer> model(@NotNull String name) {
        return Optional.ofNullable(modelOrNull(name));
    }

    /**
     * Retrieves a model renderer by its name, or null if not found.
     *
     * @param name the name of the model
     * @return the renderer, or null
     * @since 1.15.2
     */
    public static @Nullable ModelRenderer modelOrNull(@NotNull String name) {
        return plugin().modelManager().model(name);
    }

    /**
     * Retrieves a player limb renderer by its name, wrapped in an Optional.
     *
     * @param name the name of the limb model
     * @return an optional containing the renderer if found
     * @since 1.15.2
     */
    public static @NotNull Optional<ModelRenderer> limb(@NotNull String name) {
        return Optional.ofNullable(limbOrNull(name));
    }

    /**
     * Retrieves a player limb renderer by its name, or null if not found.
     *
     * @param name the name of the limb model
     * @return the renderer, or null
     * @since 1.15.2
     */
    public static @Nullable ModelRenderer limbOrNull(@NotNull String name) {
        return plugin().modelManager().limb(name);
    }

    /**
     * Retrieves a player channel handler by the player's UUID.
     *
     * @param uuid the player's UUID
     * @return an optional containing the channel handler if found
     * @since 1.15.2
     */
    public static @NotNull Optional<PlayerChannelHandler> player(@NotNull UUID uuid) {
        return Optional.ofNullable(plugin().playerManager().player(uuid));
    }

    /**
     * Retrieves an entity tracker registry by the entity's UUID.
     *
     * @param uuid the entity's UUID
     * @return an optional containing the registry if found
     * @since 1.15.2
     */
    public static @NotNull Optional<EntityTrackerRegistry> registry(@NotNull UUID uuid) {
        return Optional.ofNullable(registryOrNull(uuid));
    }

    /**
     * Retrieves an entity tracker registry for a Bukkit entity.
     *
     * @param entity the Bukkit entity
     * @return an optional containing the registry if found
     * @since 1.15.2
     */
    public static @NotNull Optional<EntityTrackerRegistry> registry(@NotNull Entity entity) {
        return Optional.ofNullable(registryOrNull(entity));
    }

    /**
     * Retrieves an entity tracker registry for a base entity.
     *
     * @param entity the base entity
     * @return an optional containing the registry if found
     * @since 1.15.2
     */
    public static @NotNull Optional<EntityTrackerRegistry> registry(@NotNull BaseEntity entity) {
        return Optional.ofNullable(registryOrNull(entity));
    }

    /**
     * Retrieves an entity tracker registry by the entity's UUID, or null if not found.
     *
     * @param uuid the entity's UUID
     * @return the registry, or null
     * @since 1.15.2
     */
    public static @Nullable EntityTrackerRegistry registryOrNull(@NotNull UUID uuid) {
        return EntityTrackerRegistry.registry(uuid);
    }

    /**
     * Retrieves an entity tracker registry for a Bukkit entity, or null if not found.
     *
     * @param entity the Bukkit entity
     * @return the registry, or null
     * @since 1.15.2
     */
    public static @Nullable EntityTrackerRegistry registryOrNull(@NotNull Entity entity) {
        return registryOrNull(nms().adapt(entity));
    }

    /**
     * Retrieves an entity tracker registry for a base entity, or null if not found.
     *
     * @param entity the base entity
     * @return the registry, or null
     * @since 1.15.2
     */
    public static @Nullable EntityTrackerRegistry registryOrNull(@NotNull BaseEntity entity) {
        return EntityTrackerRegistry.registry(entity);
    }

    /**
     * Returns a collection of all loaded model renderers.
     *
     * @return an unmodifiable collection of models
     * @since 1.15.2
     */
    public static @NotNull @Unmodifiable Collection<ModelRenderer> models() {
        return plugin().modelManager().models();
    }

    /**
     * Returns a collection of all loaded player limb renderers.
     *
     * @return an unmodifiable collection of limb models
     * @since 1.15.2
     */
    public static @NotNull @Unmodifiable Collection<ModelRenderer> limbs() {
        return plugin().modelManager().limbs();
    }

    /**
     * Returns a set of all loaded model names.
     *
     * @return an unmodifiable set of model keys
     * @since 1.15.2
     */
    public static @NotNull @Unmodifiable Set<String> modelKeys() {
        return plugin().modelManager().modelKeys();
    }

    /**
     * Returns a set of all loaded player limb model names.
     *
     * @return an unmodifiable set of limb keys
     * @since 1.15.2
     */
    public static @NotNull @Unmodifiable Set<String> limbKeys() {
        return plugin().modelManager().limbKeys();
    }

    /**
     * Returns the singleton instance of the BetterModel plugin.
     *
     * @return the plugin instance
     * @throws NullPointerException if the plugin has not been initialized
     * @see org.bukkit.plugin.java.JavaPlugin
     * @since 1.15.2
     */
    public static @NotNull BetterModelPlugin plugin() {
        return Objects.requireNonNull(instance, "BetterModel hasn't been initialized yet!");
    }

    /**
     * Returns the NMS handler instance.
     *
     * @return the NMS handler
     * @since 1.15.2
     */
    public static @NotNull NMS nms() {
        return plugin().nms();
    }

    /**
     * Registers the plugin instance.
     * <p>
     * This method is intended for internal use only during plugin initialization.
     * </p>
     *
     * @param instance the plugin instance
     * @throws RuntimeException if an instance is already registered
     * @since 1.15.2
     */
    @ApiStatus.Internal
    public static void register(@NotNull BetterModelPlugin instance) {
        Objects.requireNonNull(instance, "instance cannot be null.");
        if (BetterModel.instance == instance) throw new RuntimeException("Duplicated instance.");
        BetterModel.instance = instance;
    }
}
