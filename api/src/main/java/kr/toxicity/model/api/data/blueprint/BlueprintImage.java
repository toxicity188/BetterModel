package kr.toxicity.model.api.data.blueprint;

import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

/**
 * Blueprint image.
 * @param name image name
 * @param image buffered image
 */
public record BlueprintImage(@NotNull String name, @NotNull BufferedImage image) {
}
