package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonObject;
import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.data.raw.ModelTexture;
import kr.toxicity.model.api.util.PackUtil;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public record BlueprintTexture(
        @NotNull String name,
        BufferedImage image,
        int uvWidth,
        int uvHeight
) {
    public static @NotNull BlueprintTexture from(@NotNull ModelTexture blueprint) {
        PackUtil.validatePath(blueprint.name(), "Texture name must be [a-z0-9/._-]: " + blueprint.name());
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

    public boolean isAnimatedTexture() {
        return uvHeight != 0 && uvWidth != 0 && image.getHeight() / uvHeight / (image.getWidth() / uvWidth) > 1;
    }

    public @NotNull JsonObject toMcmeta() {
        var json = new JsonObject();
        var animation = new JsonObject();
        animation.addProperty("interpolate", true);
        animation.addProperty("frametime", 10);
        json.add("animation", animation);
        return json;
    }

    public @NotNull ModelResolution resolution() {
        return new ModelResolution(uvWidth, uvHeight);
    }
}
