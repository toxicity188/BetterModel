/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
 * A provider class for BetterModel plugin instance.
 */
public final class BetterModel {

    /**
     * Private initializer
     */
    private BetterModel() {
        throw new RuntimeException();
    }

    /**
     * Check a running platform is Folia.
     */
    public static final boolean IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    /**
     * Check a running platform is Purpur.
     */
    public static final boolean IS_PURPUR = classExists("org.purpurmc.purpur.PurpurConfig");
    /**
     * Check a running platform is Paper.
     */
    public static final boolean IS_PAPER = IS_PURPUR || IS_FOLIA || classExists("io.papermc.paper.configuration.PaperConfigurations");

    /**
     * Plugin instance.
     */
    private static BetterModelPlugin instance;

    /**
     * Gets config manager
     * @return config
     */
    public static @NotNull BetterModelConfig config() {
        return plugin().config();
    }

    /**
     * Gets model renderer by name
     * @param name name
     * @return optional renderer
     */
    public static @NotNull Optional<ModelRenderer> model(@NotNull String name) {
        return Optional.ofNullable(modelOrNull(name));
    }

    /**
     * Gets model renderer or null by name
     * @param name name
     * @return nullable renderer
     */
    public static @Nullable ModelRenderer modelOrNull(@NotNull String name) {
        return plugin().modelManager().model(name);
    }

    /**
     * Gets player animation renderer by name
     * @param name name
     * @return optional renderer
     */
    public static @NotNull Optional<ModelRenderer> limb(@NotNull String name) {
        return Optional.ofNullable(limbOrNull(name));
    }

    /**
     * Gets player animation renderer or null by name
     * @param name name
     * @return nullable renderer
     */
    public static @Nullable ModelRenderer limbOrNull(@NotNull String name) {
        return plugin().modelManager().limb(name);
    }

    /**
     * Gets player channel by uuid.
     * @param uuid uuid
     * @return optional channel
     */
    public static @NotNull Optional<PlayerChannelHandler> player(@NotNull UUID uuid) {
        return Optional.ofNullable(plugin().playerManager().player(uuid));
    }

    /**
     * Gets entity registry by entity's uuid.
     * @param uuid uuid
     * @return optional registry
     */
    public static @NotNull Optional<EntityTrackerRegistry> registry(@NotNull UUID uuid) {
        return Optional.ofNullable(registryOrNull(uuid));
    }

    /**
     * Gets entity registry by entity.
     * @param entity entity
     * @return optional registry
     */
    public static @NotNull Optional<EntityTrackerRegistry> registry(@NotNull Entity entity) {
        return Optional.ofNullable(registryOrNull(entity));
    }

    /**
     * Gets entity registry by entity.
     * @param entity entity
     * @return optional registry
     */
    public static @NotNull Optional<EntityTrackerRegistry> registry(@NotNull BaseEntity entity) {
        return Optional.ofNullable(registryOrNull(entity));
    }

    /**
     * Gets entity registry by entity's uuid.
     * @param uuid uuid
     * @return registry or null
     */
    public static @Nullable EntityTrackerRegistry registryOrNull(@NotNull UUID uuid) {
        return EntityTrackerRegistry.registry(uuid);
    }

    /**
     * Gets entity registry by entity.
     * @param entity entity
     * @return registry or null
     */
    public static @Nullable EntityTrackerRegistry registryOrNull(@NotNull Entity entity) {
        return registryOrNull(nms().adapt(entity));
    }

    /**
     * Gets entity registry by entity.
     * @param entity entity
     * @return registry or null
     */
    public static @Nullable EntityTrackerRegistry registryOrNull(@NotNull BaseEntity entity) {
        return EntityTrackerRegistry.registry(entity);
    }

    /**
     * Gets all models
     * @return all models
     */
    public static @NotNull @Unmodifiable Collection<ModelRenderer> models() {
        return plugin().modelManager().models();
    }

    /**
     * Gets all limbs
     * @return all limbs
     */
    public static @NotNull @Unmodifiable Collection<ModelRenderer> limbs() {
        return plugin().modelManager().limbs();
    }

    /**
     * Gets all keys of model
     * @return all model keys
     */
    public static @NotNull @Unmodifiable Set<String> modelKeys() {
        return plugin().modelManager().modelKeys();
    }

    /**
     * Gets all keys of limb
     * @return all limb keys
     */
    public static @NotNull @Unmodifiable Set<String> limbKeys() {
        return plugin().modelManager().limbKeys();
    }

    /**
     * Gets plugin instance of BetterModel.
     * @see org.bukkit.plugin.java.JavaPlugin
     * @return instance
     */
    public static @NotNull BetterModelPlugin plugin() {
        return Objects.requireNonNull(instance, "BetterModel hasn't been initialized yet!");
    }

    /**
     * Gets nms
     * @return nms
     */
    public static @NotNull NMS nms() {
        return plugin().nms();
    }

    /**
     * Sets plugin instance of BetterModel.
     * @param instance instance
     */
    @ApiStatus.Internal
    public static void register(@NotNull BetterModelPlugin instance) {
        Objects.requireNonNull(instance, "instance cannot be null.");
        if (BetterModel.instance == instance) throw new RuntimeException("Duplicated instance.");
        BetterModel.instance = instance;
    }
}
