/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A builder for constructing resource pack contents within a specific path context.
 * <p>
 * This class simplifies adding resources to a pack by managing the current path and providing methods to resolve sub-paths.
 * It also integrates with {@link PackObfuscator} for resource name obfuscation.
 * </p>
 *
 * @since 1.15.2
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class PackBuilder {
    private final PackAssets assets;
    private final PackPath path;
    private final PackObfuscator obfuscator = PackObfuscator.order();

    /**
     * Resolves a sub-path and returns a new builder for that path.
     *
     * @param paths the sub-path components
     * @return a new PackBuilder for the resolved path
     * @since 1.15.2
     */
    public @NotNull PackBuilder resolve(@NotNull String... paths) {
        return new PackBuilder(assets, path.resolve(paths));
    }

    /**
     * Adds a resource to the pack at the current path.
     *
     * @param path the relative path of the resource
     * @param estimatedSize the estimated size of the resource
     * @param supplier the supplier for the resource content
     * @since 1.15.2
     */
    public void add(@NotNull String path, long estimatedSize, @NotNull Supplier<byte[]> supplier) {
        add(new String[] { path }, estimatedSize, supplier);
    }

    /**
     * Adds a resource to the pack at the current path using multiple path components.
     *
     * @param paths the relative path components
     * @param size the estimated size of the resource
     * @param supplier the supplier for the resource content
     * @since 1.15.2
     */
    public void add(@NotNull String[] paths, long size, @NotNull Supplier<byte[]> supplier) {
        var resolve = path.resolve(paths);
        assets.resourceMap.putIfAbsent(resolve, PackResource.of(assets.overlay, resolve, size, supplier));
    }

    /**
     * Returns the obfuscator associated with this builder.
     *
     * @return the obfuscator
     * @since 1.15.2
     */
    public @NotNull PackObfuscator obfuscator() {
        return obfuscator;
    }

    /**
     * Adds a resource to the pack at the current path with unknown size.
     *
     * @param path the relative path of the resource
     * @param supplier the supplier for the resource content
     * @since 1.15.2
     */
    public void add(@NotNull String path, @NotNull Supplier<byte[]> supplier) {
        add(path, -1, supplier);
    }

    /**
     * Adds a resource to the pack at the current path using multiple path components with unknown size.
     *
     * @param paths the relative path components
     * @param supplier the supplier for the resource content
     * @since 1.15.2
     */
    public void add(@NotNull String[] paths, @NotNull Supplier<byte[]> supplier) {
        add(paths, -1, supplier);
    }
}
