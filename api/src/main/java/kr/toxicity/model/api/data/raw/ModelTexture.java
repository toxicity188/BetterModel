/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.data.blueprint.BlueprintTexture;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * A raw model texture.
 * @param name texture's name
 * @param source texture's base64-encoded byte array
 * @param width width
 * @param height height
 * @param uvWidth uv-width
 * @param uvHeight uv-height
 * @param frameTime frame time for animation
 * @param frameInterpolate whether to interpolate frames
 */
@ApiStatus.Internal
public record ModelTexture(
        @NotNull String name,
        @NotNull String source,
        int width,
        int height,
        @SerializedName("uv_width") int uvWidth,
        @SerializedName("uv_height") int uvHeight,
        @SerializedName("frame_time") int frameTime,
        @SerializedName("frame_interpolate") boolean frameInterpolate
) {
    /**
     * Converts this texture to blueprint textures
     * @return converted textures
     */
    public @NotNull BlueprintTexture toBlueprint() {
        var decoded = Base64.getDecoder().decode(source().substring(source().indexOf(',') + 1));
        var resolution = resolveResolution(decoded, width(), height());
        var nameIndex = name().indexOf('.');
        return new BlueprintTexture(
                nameIndex >= 0 ? name().substring(0, nameIndex) : name(),
                decoded,
                resolution[0],
                resolution[1],
                uvWidth(),
                uvHeight(),
                frameTime(),
                frameInterpolate()
        );
    }

    private static int[] resolveResolution(byte[] imageData, int currentWidth, int currentHeight) {
        if (currentWidth > 0 && currentHeight > 0) {
            return new int[]{currentWidth, currentHeight};
        }
        try (var stream = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(stream);
            if (image != null) {
                var width = currentWidth > 0 ? currentWidth : image.getWidth();
                var height = currentHeight > 0 ? currentHeight : image.getHeight();
                return new int[]{width, height};
            }
        } catch (IOException ignored) {
            // Fallback to provided dimensions if image read fails
        }
        return new int[]{
                currentWidth > 0 ? currentWidth : 0,
                currentHeight > 0 ? currentHeight : 0
        };
    }
}
