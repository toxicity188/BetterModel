package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonObject;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.data.raw.ModelTexture;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Texture of model
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
     * Gets texture from raw blueprint
     * @param blueprint raw blueprint
     * @return texture
     */
    public static @NotNull BlueprintTexture from(@NotNull ModelTexture blueprint) {
        BufferedImage image;
        try (
                var input = new ByteArrayInputStream(Base64.getDecoder().decode(blueprint.source().split(",")[1]));
                var buffered = new BufferedInputStream(input)
        ) {
            image = ImageIO.read(buffered);
        } catch (Exception e) {
            throw new RuntimeException("image");
        }
        return new BlueprintTexture(
                blueprint.name().split("\\.")[0].toLowerCase(),
                image,
                blueprint.uvWidth(),
                blueprint.uvHeight()
        );
    }

    /**
     * Checks this texture is animated
     * @return whether to animate
     */
    public boolean isAnimatedTexture() {
        return uvHeight != 0 && uvWidth != 0 && image.getHeight() / uvHeight / Math.max(image.getWidth() / uvWidth, 1) > 1;
    }

    /**
     * Generates mcmeta of this image
     * @return mcmeta
     */
    public @NotNull JsonObject toMcmeta() {
        var json = new JsonObject();
        var animation = new JsonObject();
        animation.addProperty("interpolate", true);
        animation.addProperty("frametime", BetterModel.inst().configManager().animatedTextureFrameTime());
        json.add("animation", animation);
        return json;
    }

    /**
     * Gets model resolution
     * @return resolution
     */
    public @NotNull ModelResolution resolution() {
        return new ModelResolution(uvWidth, uvHeight);
    }
}
