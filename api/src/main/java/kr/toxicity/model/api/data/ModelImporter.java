/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data;

import com.google.gson.JsonParseException;
import kr.toxicity.model.api.data.raw.ModelData;
import kr.toxicity.model.api.data.raw.ModelLoadResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Imports a raw model asset into a processed model load result.
 * Implementations must support concurrent loading and close any streams they open.
 * <p>Example: {@code manager.addImporter("custom", asset -> loadCustom(asset))}.</p>
 *
 * @since 3.7.0
 */
@FunctionalInterface
public interface ModelImporter {

    /**
     * The built-in Blockbench importer, also used when an extension has no registered importer.
     * <p>Example: {@code ModelImporter.BBMODEL.load(asset)}.</p>
     *
     * @since 3.7.0
     */
    ModelImporter BBMODEL = asset -> {
        try (
            var stream = asset.supplier().get();
            var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
        ) {
            var result = ModelData.GSON.fromJson(reader, ModelData.class);
            result.assertSupported();
            return result.loadBlueprint(asset.name());
        } catch (IOException e) {
            throw new RuntimeException("Unable to load this asset: " + asset, e);
        } catch (JsonParseException e) {
            throw new RuntimeException("Unable to parse this json asset: " + asset, e);
        }
    };

    /**
     * Loads and processes an asset.
     * <p>Example: {@code ModelLoadResult result = importer.load(asset)}.</p>
     *
     * @param asset the source asset
     * @return the processed blueprint and loading errors
     * @throws RuntimeException if reading or parsing the asset fails
     * @since 3.7.0
     */
    @NotNull ModelLoadResult load(@NotNull ModelAsset asset);
}
