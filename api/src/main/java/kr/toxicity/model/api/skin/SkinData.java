package kr.toxicity.model.api.skin;

import kr.toxicity.model.api.util.TransformedItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Skin data of player.
 */
public interface SkinData {
    /**
     * Gets head part
     * @return head
     */
    @NotNull TransformedItemStack head();
    /**
     * Gets hip part
     * @return hip
     */
    @NotNull TransformedItemStack hip();
    /**
     * Gets waist part
     * @return waist
     */
    @NotNull TransformedItemStack waist();
    /**
     * Gets chest part
     * @return chest
     */
    @NotNull TransformedItemStack chest();
    /**
     * Gets left arm part
     * @return left arm
     */
    @NotNull TransformedItemStack leftArm();
    /**
     * Gets left forearm part
     * @return left forearm
     */
    @NotNull TransformedItemStack leftForeArm();
    /**
     * Gets right arm part
     * @return right arm
     */
    @NotNull TransformedItemStack rightArm();
    /**
     * Gets right forearm part
     * @return right forearm
     */
    @NotNull TransformedItemStack rightForeArm();
    /**
     * Gets left leg part
     * @return left leg
     */
    @NotNull TransformedItemStack leftLeg();
    /**
     * Gets left foreleg part
     * @return left foreleg
     */
    @NotNull TransformedItemStack leftForeLeg();
    /**
     * Gets right leg part
     * @return right leg
     */
    @NotNull TransformedItemStack rightLeg();
    /**
     * Gets right foreleg part
     * @return right foreleg
     */
    @NotNull TransformedItemStack rightForeLeg();
}
