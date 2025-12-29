/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents a resource within a resource pack.
 * <p>
 * A resource consists of its content (as a byte array), its path, and optionally the overlay it belongs to.
 * </p>
 *
 * @since 1.15.2
 */
public interface PackResource extends Supplier<byte[]> {

    /**
     * Returns the overlay this resource belongs to.
     *
     * @return the overlay, or null if it belongs to the base pack
     * @since 1.15.2
     */
    @Nullable
    PackOverlay overlay();

    /**
     * Returns the path of this resource.
     *
     * @return the pack path
     * @since 1.15.2
     */
    @NotNull
    PackPath path();

    /**
     * Returns the estimated size of this resource in bytes.
     *
     * @return the estimated size
     * @since 1.15.2
     */
    long estimatedSize();

    /**
     * Creates a new pack resource for the base pack.
     *
     * @param path the path of the resource
     * @param size the estimated size
     * @param supplier the content supplier
     * @return the created resource
     * @since 1.15.2
     */
    static @NotNull PackResource of(@NotNull PackPath path, long size, @NotNull Supplier<byte[]> supplier) {
        return of(null, path, size, supplier);
    }

    /**
     * Creates a new pack resource for a specific overlay.
     *
     * @param overlay the overlay (or null for base pack)
     * @param path the path of the resource
     * @param size the estimated size
     * @param supplier the content supplier
     * @return the created resource
     * @since 1.15.2
     */
    static @NotNull PackResource of(@Nullable PackOverlay overlay, @NotNull PackPath path, long size, @NotNull Supplier<byte[]> supplier) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(supplier, "supplier");
        return new Packed(overlay, path, size, supplier);
    }

    /**
     * A simple implementation of {@link PackResource}.
     *
     * @param overlay the overlay
     * @param path the path
     * @param estimatedSize the estimated size
     * @param supplier the content supplier
     * @since 1.15.2
     */
    record Packed(
        @Nullable PackOverlay overlay,
        @NotNull PackPath path,
        long estimatedSize,
        @NotNull Supplier<byte[]> supplier
    ) implements PackResource {
        @Override
        public byte[] get() {
            return supplier.get();
        }
    }
}
