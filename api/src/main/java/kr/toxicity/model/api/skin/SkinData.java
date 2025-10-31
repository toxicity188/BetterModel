/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.skin;

import kr.toxicity.model.api.nms.Profiled;
import kr.toxicity.model.api.util.TransformedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Skin data of player.
 */
public interface SkinData {
    /**
     * Gets head part
     * @param profiled profiled
     * @return head
     */
    @NotNull TransformedItemStack head(@NotNull Profiled profiled);
    /**
     * Gets hip part
     * @param profiled profiled
     * @return hip
     */
    @NotNull TransformedItemStack hip(@NotNull Profiled profiled);
    /**
     * Gets waist part
     * @param profiled profiled
     * @return waist
     */
    @NotNull TransformedItemStack waist(@NotNull Profiled profiled);
    /**
     * Gets chest part
     * @param profiled profiled
     * @return chest
     */
    @NotNull TransformedItemStack chest(@NotNull Profiled profiled);
    /**
     * Gets left arm part
     * @param profiled profiled
     * @return left arm
     */
    @NotNull TransformedItemStack leftArm(@NotNull Profiled profiled);
    /**
     * Gets left forearm part
     * @return left forearm
     */
    @NotNull TransformedItemStack leftForeArm();
    /**
     * Gets right arm part
     * @param profiled profiled
     * @return right arm
     */
    @NotNull TransformedItemStack rightArm(@NotNull Profiled profiled);
    /**
     * Gets right forearm part
     * @return right forearm
     */
    @NotNull TransformedItemStack rightForeArm();
    /**
     * Gets left leg part
     * @param profiled profiled
     * @return left leg
     */
    @NotNull TransformedItemStack leftLeg(@NotNull Profiled profiled);
    /**
     * Gets left foreleg part
     * @param profiled profiled
     * @return left foreleg
     */
    @NotNull TransformedItemStack leftForeLeg(@NotNull Profiled profiled);
    /**
     * Gets right leg part
     * @param profiled profiled
     * @return right leg
     */
    @NotNull TransformedItemStack rightLeg(@NotNull Profiled profiled);
    /**
     * Gets right foreleg part
     * @param profiled profiled
     * @return right foreleg
     */
    @NotNull TransformedItemStack rightForeLeg(@NotNull Profiled profiled);
    /**
     * Gets cape
     * @param profiled profiled
     * @return cape
     */
    @Nullable TransformedItemStack cape(@NotNull Profiled profiled);
}
