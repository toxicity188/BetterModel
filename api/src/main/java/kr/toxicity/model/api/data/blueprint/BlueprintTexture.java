package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelTexture;
import kr.toxicity.model.api.util.MathUtil;
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

    public double resolution(double resolution) {
        var imageResolution = MathUtil.resolution(uvWidth, uvHeight);
        return imageResolution > 0 ? imageResolution : resolution;
    }
}
