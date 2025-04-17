package kr.toxicity.model.api;

import kr.toxicity.model.api.event.PluginEndReloadEvent;
import kr.toxicity.model.api.event.PluginStartReloadEvent;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.util.EventUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static kr.toxicity.model.api.util.ReflectionUtil.*;

/**
 * A dummy class for BetterModel plugin instance.
 */
public final class BetterModel {

    /**
     * Private initializer
     */
    private BetterModel() {}

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
     * Gets plugin instance of BetterModel.
     * @see org.bukkit.plugin.java.JavaPlugin
     * @return instance
     */
    public static @NotNull BetterModelPlugin inst() {
        return Objects.requireNonNull(instance);
    }

    /**
     * Sets plugin instance of BetterModel.
     * @param instance instance
     */
    @ApiStatus.Internal
    public static void inst(@NotNull BetterModelPlugin instance) {
        if (BetterModel.instance != null) throw new RuntimeException();
        instance.addReloadStartHandler(() -> EventUtil.call(new PluginStartReloadEvent()));
        instance.addReloadEndHandler(t -> EntityTracker.reload());
        instance.addReloadEndHandler(t -> EventUtil.call(new PluginEndReloadEvent(t)));
        BetterModel.instance = instance;
    }
}
