/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import kr.toxicity.model.api.BetterModel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Manages assets within a specific pack overlay.
 * <p>
 * This class provides access to namespaces (like 'bettermodel' and 'minecraft') and allows adding resources to the pack.
 * </p>
 *
 * @since 1.15.2
 */
public final class PackAssets {
    final PackPath path;
    final PackOverlay overlay;
    final Map<PackPath, PackResource> resourceMap = new ConcurrentHashMap<>();

    private final PackNamespace bettermodel, minecraft;

    PackAssets(@NotNull PackOverlay overlay) {
        this.overlay = overlay;
        this.path = overlay.path(BetterModel.config().namespace());
        bettermodel = new PackNamespace(this, BetterModel.config().namespace());
        minecraft = new PackNamespace(this, "minecraft");
    }

    /**
     * Returns the 'bettermodel' namespace (or the configured namespace).
     *
     * @return the namespace
     * @since 1.15.2
     */
    public @NotNull PackNamespace bettermodel() {
        return bettermodel;
    }

    /**
     * Returns the 'minecraft' namespace.
     *
     * @return the namespace
     * @since 1.15.2
     */
    public @NotNull PackNamespace minecraft() {
        return minecraft;
    }

    int size() {
        return resourceMap.size();
    }

    boolean dirty() {
        return size() > 0;
    }

    /**
     * Adds a resource to the pack.
     *
     * @param path the path of the resource
     * @param size the estimated size of the resource
     * @param supplier the supplier for the resource content
     * @since 1.15.2
     */
    public void add(@NotNull String path, long size, @NotNull Supplier<byte[]> supplier) {
        add(new String[] { path }, size, supplier);
    }

    /**
     * Adds a resource to the pack using multiple path components.
     *
     * @param paths the path components
     * @param size the estimated size of the resource
     * @param supplier the supplier for the resource content
     * @since 1.15.2
     */
    public void add(@NotNull String[] paths, long size, @NotNull Supplier<byte[]> supplier) {
        var resolve = path.resolve(paths);
        resourceMap.putIfAbsent(resolve, PackResource.of(overlay, resolve, size, supplier));
    }

    /**
     * Adds a resource to the pack with unknown size.
     *
     * @param path the path of the resource
     * @param supplier the supplier for the resource content
     * @since 1.15.2
     */
    public void add(@NotNull String path, @NotNull Supplier<byte[]> supplier) {
        add(path, -1, supplier);
    }

    /**
     * Adds a resource to the pack using multiple path components with unknown size.
     *
     * @param paths the path components
     * @param supplier the supplier for the resource content
     * @since 1.15.2
     */
    public void add(@NotNull String[] paths, @NotNull Supplier<byte[]> supplier) {
        add(paths, -1, supplier);
    }
}
