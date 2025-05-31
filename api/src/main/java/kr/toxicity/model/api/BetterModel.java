package kr.toxicity.model.api;

import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.event.PluginEndReloadEvent;
import kr.toxicity.model.api.event.PluginStartReloadEvent;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.util.EventUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
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
     * Gets model render by name
     * @param name name
     * @return optional renderer
     */
    public static @NotNull Optional<ModelRenderer> model(@NotNull String name) {
        return Optional.ofNullable(plugin().modelManager().renderer(name));
    }
    /**
     * Gets player animation render by name
     * @param name name
     * @return optional renderer
     */
    public static @NotNull Optional<ModelRenderer> limb(@NotNull String name) {
        return Optional.ofNullable(plugin().playerManager().limb(name));
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
     * Gets all models
     * @return all models
     */
    public static @NotNull @Unmodifiable List<ModelRenderer> models() {
        return plugin().modelManager().renderers();
    }
    /**
     * Gets all limbs
     * @return all limbs
     */
    public static @NotNull @Unmodifiable List<ModelRenderer> limbs() {
        return plugin().playerManager().limbs();
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
        instance.addReloadStartHandler(() -> EventUtil.call(new PluginStartReloadEvent()));
        instance.addReloadEndHandler(t -> EntityTracker.reload());
        instance.addReloadEndHandler(t -> EventUtil.call(new PluginEndReloadEvent(t)));
        BetterModel.instance = Objects.requireNonNull(instance);
    }
}
