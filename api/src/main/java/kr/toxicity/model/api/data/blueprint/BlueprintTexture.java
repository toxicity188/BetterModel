/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonObject;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.util.PackUtil;
import kr.toxicity.model.api.util.json.JsonObjectBuilder;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

/**
 * Texture of the model
 * @param name texture name
 * @param image image
 * @param uvWidth uv width
 * @param uvHeight uv height
 */
public record BlueprintTexture(
        @NotNull String name,
        BufferedImage image,
        int uvWidth,
        int uvHeight
) {
    /**
     * Checks this texture is animated
     * @return whether to animate
     */
    public boolean isAnimatedTexture() {
        if (uvWidth > 0 && uvHeight > 0) {
            var h = (float) image.getHeight() / uvHeight;
            var w = (float) image.getWidth() / uvWidth;
            return h > w;
        } else {
            return image.getHeight() / image.getWidth() > 1;
        }
    }

    /**
     * Checks this textures can be rendered.
     * @return can be rendered.
     */
    public boolean canBeRendered() {
        return !name.startsWith("-");
    }

    /**
     * Generates mcmeta of this image
     * @return mcmeta
     */
    public @NotNull JsonObject toMcmeta() {
        return JsonObjectBuilder.builder()
                .jsonObject("animation", animation -> {
                    animation.property("interpolate", true);
                    animation.property("frametime", BetterModel.config().animatedTextureFrameTime());
                })
                .build();
    }

    /**
     * Creates pack name
     * @param parentName parent name
     * @return texture name in pack
     */
    public @NotNull String packName(@NotNull String parentName) {
        return BetterModel.config().namespace() + ":item/" + PackUtil.toPackName(parentName + "_" + name());
    }

    /**
     * Gets model resolution
     * @return resolution
     */
    public @NotNull ModelResolution resolution() {
        return new ModelResolution(uvWidth, uvHeight);
    }
}
