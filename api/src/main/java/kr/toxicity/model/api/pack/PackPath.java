/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

import static java.lang.String.join;

/**
 * Represents a path within a resource pack.
 * <p>
 * This record encapsulates a string path and provides utility methods for resolving sub-paths.
 * It implements {@link Comparable} for sorting purposes.
 * </p>
 *
 * @param path the string representation of the path
 * @since 1.15.2
 */
public record PackPath(@NotNull String path) implements Comparable<PackPath> {

    /**
     * The delimiter used for path separation.
     * @since 1.15.2
     */
    public static final String DELIMITER = "/";

    /**
     * An empty pack path.
     * @since 1.15.2
     */
    public static final PackPath EMPTY = new PackPath("");

    /**
     * Resolves a sub-path relative to this path.
     *
     * @param subPaths the sub-path components to resolve
     * @return the resolved pack path
     * @since 1.15.2
     */
    public @NotNull PackPath resolve(@NotNull String... subPaths) {
        if (subPaths.length == 0) return this;
        return new PackPath(path.isEmpty() ? join(DELIMITER, subPaths) : path + DELIMITER + join(DELIMITER, subPaths));
    }

    @Override
    public @NotNull String toString() {
        return path;
    }

    @Override
    public int compareTo(@NotNull PackPath o) {
        return path.compareTo(o.path);
    }
}
