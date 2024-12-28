package kr.toxicity.model.api;

import kr.toxicity.model.api.manager.*;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.scheduler.ModelScheduler;
import kr.toxicity.model.api.version.MinecraftVersion;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class BetterModel extends JavaPlugin {

    public static final boolean IS_PAPER;

    private static BetterModel instance;

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

    @Override
    public final void onLoad() {
        if (instance != null) throw new RuntimeException();
        instance = this;
    }

    public static @NotNull BetterModel inst() {
        return Objects.requireNonNull(instance);
    }

    public abstract @NotNull ReloadResult reload();

    public abstract @NotNull MinecraftVersion version();
    public abstract @NotNull NMS nms();

    public abstract @NotNull ModelManager modelManager();
    public abstract @NotNull PlayerManager playerManager();
    public abstract @NotNull EntityManager entityManager();
    public abstract @NotNull CommandManager commandManager();
    public abstract @NotNull CompatibilityManager compatibilityManager();
    public abstract @NotNull ConfigManager configManager();
    public abstract @NotNull ModelScheduler scheduler();

    public sealed interface ReloadResult {
        record Success(long time) implements ReloadResult {
        }

        record OnReload() implements ReloadResult {
        }

        record Failure(@NotNull Throwable throwable) implements ReloadResult {
        }
    }
}
