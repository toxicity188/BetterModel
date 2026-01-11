package kr.toxicity.model;

import kr.toxicity.model.api.BetterModelPlatform;
import org.jetbrains.annotations.NotNull;

public interface BetterModelPlatformImpl extends BetterModelPlatform {
    void saveResource(@NotNull String resourcePath);
}
