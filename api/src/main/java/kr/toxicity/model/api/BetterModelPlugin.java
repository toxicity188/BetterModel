package kr.toxicity.model.api;

import kr.toxicity.model.api.manager.*;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.scheduler.ModelScheduler;
import kr.toxicity.model.api.version.MinecraftVersion;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface BetterModelPlugin {

    @NotNull ReloadResult reload();

    @NotNull MinecraftVersion version();
    @NotNull NMS nms();

    @NotNull ModelManager modelManager();
    @NotNull PlayerManager playerManager();
    @NotNull EntityManager entityManager();
    @NotNull CommandManager commandManager();
    @NotNull CompatibilityManager compatibilityManager();
    @NotNull ConfigManager configManager();
    @NotNull ModelScheduler scheduler();

    void addReloadStartHandler(@NotNull Runnable runnable);
    void addReloadEndHandler(@NotNull Consumer<ReloadResult> consumer);

    sealed interface ReloadResult {
        record Success(long time) implements ReloadResult {
        }

        record OnReload() implements ReloadResult {
        }

        record Failure(@NotNull Throwable throwable) implements ReloadResult {
        }
    }
}
