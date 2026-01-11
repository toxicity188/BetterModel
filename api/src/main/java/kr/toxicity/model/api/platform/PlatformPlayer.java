package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;

public interface PlatformPlayer extends PlatformLivingEntity, PlatformOfflinePlayer {

    @Override
    @NotNull String name();
}
