/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

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
