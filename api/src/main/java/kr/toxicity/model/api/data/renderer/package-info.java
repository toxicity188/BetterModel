/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines renderer-facing model data and render pipeline contracts.
 * <p>
 * Renderer types expose loaded model renderers, renderer groups, render sources,
 * and the per-tracker pipeline that owns rendered bones. They describe the
 * packet-based rendering domain without leaking platform-specific entity types.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ModelRenderer renderer = BetterModel.model("example_model").orElseThrow();
 * EntityTracker tracker = renderer.create(entity);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.data.renderer;
