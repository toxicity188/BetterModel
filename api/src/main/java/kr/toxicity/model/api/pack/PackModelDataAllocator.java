/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Defines a strategy for numbering the model parts of the pack.
 * <p>
 * The number becomes the item model's threshold and the item's custom_model_data.
 * </p>
 *
 * @since 3.4.0
 */
@FunctionalInterface
public interface PackModelDataAllocator {

    /**
     * Allocates the custom_model_data of the given model part.
     *
     * @param modelPart the pack name of the model part
     * @return the custom_model_data
     * @since 3.4.0
     */
    int allocate(@NotNull String modelPart);

    /**
     * Creates an order-based allocator.
     *
     * @return the allocator
     * @since 3.4.0
     */
    static @NotNull PackModelDataAllocator order() {
        return new Order();
    }

    /**
     * An allocator that numbers model parts from one based on the order of appearance.
     *
     * @since 3.4.0
     */
    final class Order implements PackModelDataAllocator {

        private final AtomicInteger indexer = new AtomicInteger(1);

        /**
         * Private initializer
         */
        private Order() {
        }

        public int allocate(@NotNull String modelPart) {
            return indexer.getAndIncrement();
        }
    }
}
