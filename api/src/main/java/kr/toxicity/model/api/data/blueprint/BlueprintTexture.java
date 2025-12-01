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

/**
 * Texture of the model
 * @param name texture name
 * @param image image
 * @param width original width
 * @param height original height
 * @param uvWidth uv width
 * @param uvHeight uv height
 * @param frameTime frame time for animation
 * @param frameInterpolate whether to interpolate frames
 */
public record BlueprintTexture(
        @NotNull String name,
        byte[] image,
        int width,
        int height,
        int uvWidth,
        int uvHeight,
        int frameTime,
        boolean frameInterpolate
) {
    /**
     * Checks this texture is animated
     * @return whether to animate
     */
    public boolean isAnimatedTexture() {
        if (uvWidth > 0 && uvHeight > 0) {
            var h = (float) height / uvHeight;
            var w = (float) width / uvWidth;
            return h > w;
        } else {
            return height > 0 && width > 0 && height / width > 1;
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
                    animation.property("interpolate", frameInterpolate());
                    int frametime = frameTime() > 0 ? frameTime() : BetterModel.config().animatedTextureFrameTime();
                    animation.property("frametime", frametime);
                })
                .build();
    }

    /**
     * Gets pack name
     * @param parent parent
     * @return pack name
     */
    public @NotNull String packName(@NotNull String parent) {
        return PackUtil.toPackName(name().startsWith("global_") ? name() : parent + "_" + name());
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
     * Checks this textures has UV size
     * @return has UV size
     */
    public boolean hasUVSize() {
        return uvWidth > 0 && uvHeight > 0;
    }

    /**
     * Gets model resolution
     * @param resolution parent resolution
     * @return resolution
     */
    public @NotNull ModelResolution resolution(@NotNull ModelResolution resolution) {
        if (!hasUVSize()) return resolution;
        return resolution.width() == width && resolution.height() == height ? resolution : new ModelResolution(uvWidth, uvHeight);
    }
}
