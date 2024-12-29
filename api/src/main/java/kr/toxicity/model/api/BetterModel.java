package kr.toxicity.model.api;

import kr.toxicity.model.api.tracker.EntityTracker;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BetterModel {
    public static final boolean IS_PAPER;

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

    public static @NotNull BetterModelPlugin inst() {
        return Objects.requireNonNull(instance);
    }
    @ApiStatus.Internal
    public static void inst(@NotNull BetterModelPlugin instance) {
        if (BetterModel.instance != null) throw new RuntimeException();
        instance.addReloadEndHandler(t -> EntityTracker.reload());
        BetterModel.instance = instance;
    }
}
