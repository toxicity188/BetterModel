package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.data.blueprint.BlueprintTexture;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * A raw model texture.
 * @param name texture's name
 * @param source texture's base64-encoded byte array
 * @param width width
 * @param height height
 * @param uvWidth uv-width
 * @param uvHeight uv-height
 */
@ApiStatus.Internal
public record ModelTexture(
        @NotNull String name,
        @NotNull String source,
        int width,
        int height,
        @SerializedName("uv_width") int uvWidth,
        @SerializedName("uv_height") int uvHeight
) {
    /**
     * Converts this texture to blueprint textures
     * @return converted textures
     */
    public @NotNull BlueprintTexture toBlueprint() {
        BufferedImage image;
        try (
                var input = new ByteArrayInputStream(Base64.getDecoder().decode(source().split(",")[1]))
        ) {
            image = ImageIO.read(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new BlueprintTexture(
                name().split("\\.")[0],
                image,
                uvWidth(),
                uvHeight()
        );
    }
}
