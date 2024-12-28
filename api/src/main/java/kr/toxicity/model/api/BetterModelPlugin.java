package kr.toxicity.model.api;

import kr.toxicity.model.api.manager.*;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.scheduler.ModelScheduler;
import kr.toxicity.model.api.version.MinecraftVersion;
import org.jetbrains.annotations.NotNull;

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

    sealed interface ReloadResult {
        record Success(long time) implements ReloadResult {
        }

        record OnReload() implements ReloadResult {
        }

        record Failure(@NotNull Throwable throwable) implements ReloadResult {
        }
    }
}
