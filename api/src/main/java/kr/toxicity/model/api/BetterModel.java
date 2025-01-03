package kr.toxicity.model.api;

import kr.toxicity.model.api.tracker.EntityTracker;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A dummy class for BetterModel plugin instance.
 */
public class BetterModel {
    /**
     * Checks running platform is Paper.
     */
    public static final boolean IS_PAPER;

    /**
     * Plugin instance.
     */
    private static BetterModelPlugin instance;

    static {
        boolean paper;
        try {
            Class.forName("io.papermc.paper.configuration.PaperConfigurations");
            paper = true;
        } catch (Exception e) {
            paper = false;
        }
        IS_PAPER = paper;
    }

    /**
     * Gets plugin instance of BetterModel.
     * @see BetterModelPlugin
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
        instance.addReloadEndHandler(t -> EntityTracker.reload());
        BetterModel.instance = instance;
    }
}
