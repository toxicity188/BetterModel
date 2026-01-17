/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
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
