package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlatformAdapter {

    int serverViewDistance();

    @Nullable PlatformPlayer player(@NotNull UUID uuid);

    @NotNull PlatformOfflinePlayer offlinePlayer(@NotNull UUID uuid);

    @NotNull PlatformItemStack air();
}
