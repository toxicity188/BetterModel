package kr.toxicity.model;

import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.manager.ReloadPipeline;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.function.BiConsumer;

public interface BetterModelPlatformImpl extends BetterModelPlatform {

    void saveResource(@NotNull String resourcePath);

    void loadAssets(@NotNull ReloadPipeline pipeline, @NotNull String prefix, @NotNull BiConsumer<String, InputStream> consumer);
}
