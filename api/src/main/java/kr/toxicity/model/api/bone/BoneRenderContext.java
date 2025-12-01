/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.skin.SkinData;
import org.jetbrains.annotations.NotNull;

/**
 * Render item context
 * @param source source
 * @param skin skin
 */
public record BoneRenderContext(@NotNull RenderSource<?> source, @NotNull SkinData skin) {

    /**
     * Creates default context
     * @param source source
     */
    public BoneRenderContext(@NotNull RenderSource<?> source) {
        this(source, BetterModel.plugin().skinManager().fallback());
    }
}
