package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlatformOfflinePlayer {

    @NotNull UUID uuid();

    @Nullable String name();
}
