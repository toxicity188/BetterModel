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
import kr.toxicity.model.api.pack.PackObfuscator;
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
        @NotNull BufferedImage image,
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
     * Gets pack name
     * @param parent parent
     * @return pack name
     */
    public @NotNull String packName(@NotNull String parent) {
        return PackUtil.toPackName(parent + "_" + name());
    }

    /**
     * Creates pack name
     * @param obfuscator obfuscator
     * @param parent parent
     * @return pack name
     */
    public @NotNull String packName(@NotNull PackObfuscator obfuscator, @NotNull String parent) {
        return obfuscator.obfuscate(packName(parent));
    }

    /**
     * Creates pack namespace
     * @param obfuscator obfuscator
     * @param parentName parent name
     * @return texture namespace
     */
    public @NotNull String packNamespace(@NotNull PackObfuscator obfuscator, @NotNull String parentName) {
        return BetterModel.config().namespace() + ":item/" + packName(obfuscator, parentName);
    }

    /**
     * Gets model resolution
     * @param resolution parent resolution
     * @return resolution
     */
    public @NotNull ModelResolution resolution(@NotNull ModelResolution resolution) {
        return resolution.width() == image.getWidth() && resolution.height() == image.getHeight() ? new ModelResolution(image.getWidth(), image.getHeight()) : new ModelResolution(uvWidth, uvHeight);
    }
}
