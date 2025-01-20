package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.data.raw.ModelTexture;
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
                blueprint.name().split("\\.")[0],
                image,
                blueprint.uvWidth(),
                blueprint.uvHeight()
        );
    }

    public @NotNull ModelResolution resolution() {
        return new ModelResolution(uvWidth, uvHeight);
    }
}
