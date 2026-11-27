/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a mutable item stack in the underlying platform.
 * <p>
 * This interface provides methods for manipulating item properties like the item model for resource-pack rendering,
 * enchantment glint, and cloning. Clone the stack before editing if the original must be preserved.
 * </p>
 *
 * <pre>{@code
 * var preview = stack.clone().enchant(true);
 * }</pre>
 *
 * @since 2.0.0
 */
public interface PlatformItemStack {

    /**
     * Checks if the item stack is empty or air.
     *
     * @return whether this stack is empty or represents air
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
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull PlatformItemStack enchant(boolean enchant);

    /**
     * Sets the resource-pack item model on this stack.
     *
     * @param namespace the item model identifier, or {@code null} to clear the override
     * @return this item stack
     * @since 3.4.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull PlatformItemStack itemModel(@Nullable PlatformNamespace namespace);

    /**
     * Creates a copy of this item stack.
     *
     * @return the cloned item stack
     * @since 2.0.0
     */
    @NotNull PlatformItemStack clone();
}
