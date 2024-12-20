package kr.toxicity.model.api;

import kr.toxicity.model.api.manager.ModelManager;
import kr.toxicity.model.api.manager.PlayerManager;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.version.MinecraftVersion;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class ModelRenderer extends JavaPlugin {

    private static ModelRenderer instance;

    @Override
    public void onLoad() {
        if (instance != null) throw new RuntimeException();
        instance = this;
    }

    public static @NotNull ModelRenderer inst() {
        return Objects.requireNonNull(instance);
    }

    public abstract @NotNull ReloadResult reload();

    public abstract @NotNull MinecraftVersion version();
    public abstract @NotNull NMS nms();

    public abstract @NotNull ModelManager modelManager();
    public abstract @NotNull PlayerManager playerManager();

    public sealed interface ReloadResult {
        record Success(long time) implements ReloadResult {
        }

        record OnReload() implements ReloadResult {
        }

        record Failure(@NotNull Throwable throwable) implements ReloadResult {
        }
    }
}
