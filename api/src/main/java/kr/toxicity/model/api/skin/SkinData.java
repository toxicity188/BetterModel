package kr.toxicity.model.api.skin;

import kr.toxicity.model.api.util.TransformedItemStack;
import org.jetbrains.annotations.NotNull;

public interface SkinData {
    @NotNull TransformedItemStack head();
    @NotNull TransformedItemStack hip();
    @NotNull TransformedItemStack waist();
    @NotNull TransformedItemStack chest();
    @NotNull TransformedItemStack leftArm();
    @NotNull TransformedItemStack leftForeArm();
    @NotNull TransformedItemStack rightArm();
    @NotNull TransformedItemStack rightForeArm();
    @NotNull TransformedItemStack leftLeg();
    @NotNull TransformedItemStack leftForeLeg();
    @NotNull TransformedItemStack rightLeg();
    @NotNull TransformedItemStack rightForeLeg();
}
