/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a raw byte array associated with a specific pack path.
 * <p>
 * This record is used to store the binary content of a resource within a resource pack.
 * It implements {@link Comparable} to allow sorting based on the path.
 * </p>
 *
 * @param path the path of the resource
 * @param bytes the binary content of the resource
 * @since 1.15.2
 */
public record PackByte(@NotNull PackPath path, byte[] bytes) implements Comparable<PackByte> {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PackByte packByte)) return false;
        return path.equals(packByte.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public int compareTo(@NotNull PackByte o) {
        return path.compareTo(o.path);
    }
}
