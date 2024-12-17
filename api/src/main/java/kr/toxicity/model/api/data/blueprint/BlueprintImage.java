package kr.toxicity.model.api.data.blueprint;

import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

public record BlueprintImage(@NotNull String name, @NotNull BufferedImage image) {
}
