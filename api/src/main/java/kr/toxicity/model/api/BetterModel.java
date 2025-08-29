package kr.toxicity.model.api;

import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.event.PluginEndReloadEvent;
import kr.toxicity.model.api.event.PluginStartReloadEvent;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import kr.toxicity.model.api.util.EventUtil;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
        return Optional.ofNullable(EntityTrackerRegistry.registry(uuid));
    }

    /**
     * Gets entity registry by entity.
     * @param entity entity
     * @return optional registry
     */
    public static @NotNull Optional<EntityTrackerRegistry> registry(@NotNull Entity entity) {
        return Optional.ofNullable(EntityTrackerRegistry.registry(entity));
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
     * Gets plugin instance of BetterModel.
     * @see org.bukkit.plugin.java.JavaPlugin
     * @return instance
     */
    public static @NotNull BetterModelPlugin plugin() {
        return Objects.requireNonNull(instance, "BetterModel hasn't been initialized yet!");
    }

    @Deprecated
    public static @NotNull BetterModelPlugin inst() {
        return plugin();
    }

    /**
     * Sets plugin instance of BetterModel.
     * @param instance instance
     */
    @ApiStatus.Internal
    public static void register(@NotNull BetterModelPlugin instance) {
        Objects.requireNonNull(instance, "instance cannot be null.");
        if (BetterModel.instance == instance) throw new RuntimeException("Duplicated instance.");
        instance.addReloadStartHandler(zipper -> EventUtil.call(new PluginStartReloadEvent(zipper)));
        instance.addReloadEndHandler(t -> EventUtil.call(new PluginEndReloadEvent(t)));
        BetterModel.instance = instance;
    }
}
