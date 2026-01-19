/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an item stack in the underlying platform.
 * <p>
 * This interface provides methods for manipulating item properties like custom model data,
 * enchantment glint, and cloning.
 * </p>
 *
 * @since 2.0.0
 */
public interface PlatformItemStack {

    /**
     * Checks if the item stack is empty or air.
     *
     * @return true if air, false otherwise
     * @since 2.0.0
     */
    boolean isAir();

    /**
     * Sets the enchantment glint override for the item.
     *
     * @param enchant true to enable glint, false to disable
     * @return this item stack
     * @since 2.0.0
     */
    @NotNull PlatformItemStack enchant(boolean enchant);

    /**
     * Sets the custom model data and item model namespace for the item.
     *
     * @param customModelData the custom model data integer
     * @param namespace the item model namespace (optional)
     * @return this item stack
     * @since 2.0.0
     */
    @NotNull PlatformItemStack modelData(int customModelData, @Nullable PlatformNamespace namespace);

    /**
     * Creates a copy of this item stack.
     *
     * @return the cloned item stack
     * @since 2.0.0
     */
    @NotNull PlatformItemStack clone();
}
