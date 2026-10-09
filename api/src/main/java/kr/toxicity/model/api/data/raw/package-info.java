/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Models raw imported model data before it is converted into a blueprint.
 * <p>
 * The package contains records for Blockbench-like model metadata, textures,
 * UVs, outliner entries, animation channels, keyframes, and load results. It is
 * intended for parsers and import pipelines that need a stable data shape before
 * renderer-ready normalization.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ModelData data = ModelData.GSON.fromJson(json, ModelData.class);
 * ModelLoadResult result = data.loadBlueprint("example_model");
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.data.raw;
