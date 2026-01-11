/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.platform.PlatformItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Function;

/**
 * ItemStack with offset and scale
 * @see PlatformItemStack
 * @param position global position (x, y, z)
 * @param offset offset (x, y, z)
 * @param scale scale (x, y, z)
 * @param itemStack item
 */
public record TransformedItemStack(@NotNull Vector3f position, @NotNull Vector3f offset, @NotNull Vector3f scale, @NotNull PlatformItemStack itemStack) {

    /**
     * Creates empty transformed item
     * @return empty transformed item
     */
    public static @NotNull TransformedItemStack empty() {
        return of(BetterModel.platform().adapter().air());
    }

    /**
     * Creates transformed item
     * @param itemStack item
     * @return transformed item
     */
    public static @NotNull TransformedItemStack of(@NotNull PlatformItemStack itemStack) {
        return of(new Vector3f(), new Vector3f(), new Vector3f(1), itemStack);
    }

    /**
     * Creates transformed item
     * @param position position
     * @param offset offset
     * @param scale scale
     * @param itemStack item
     * @return transformed item
     */
    public static @NotNull TransformedItemStack of(@NotNull Vector3f position, @NotNull Vector3f offset, @NotNull Vector3f scale, @NotNull PlatformItemStack itemStack) {
        return new TransformedItemStack(position, offset, scale, itemStack);
    }

    /**
     * Gets transformed item as air
     * @return air item
     */
    public @NotNull TransformedItemStack asAir() {
        return of(position, offset, scale, BetterModel.platform().adapter().air());
    }

    /**
     * Sets offset
     * @param offset offset
     * @return new item
     */
    public @NotNull TransformedItemStack offset(@NotNull Vector3f offset) {
        return of(position, offset, scale, itemStack);
    }

    /**
     * Modify item
     * @param mapper mapper
     * @return modified item
     */
    public @NotNull TransformedItemStack modify(@NotNull Function<PlatformItemStack, PlatformItemStack> mapper) {
        return of(position, offset, scale, mapper.apply(itemStack.clone()));
    }

    /**
     * Checks this item is air
     * @return is air
     */
    public boolean isAir() {
        return itemStack.isAir();
    }

    /**
     * Copy this item
     * @return copied item
     */
    public @NotNull TransformedItemStack copy() {
        return new TransformedItemStack(
            new Vector3f(position),
            new Vector3f(offset),
            new Vector3f(scale),
            itemStack.clone()
        );
    }
}
