/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;
import java.util.Set;

/**
 * A tagged name of some bone
 * @param tags tags
 * @param name name
 * @param rawName original name
 */
public record BoneName(@NotNull @Unmodifiable Set<BoneTag> tags, @NotNull String name, @NotNull String rawName) {

    /**
     * Checks this name has some tags
     * @param tags tags
     * @return any match
     */
    public boolean tagged(@NotNull BoneTag... tags) {
        for (BoneTag boneTag : tags) {
            if (this.tags.contains(boneTag)) return true;
        }
        return false;
    }

    /**
     * Gets an item mapper of this bone name.
     * @return item mapper
     */
    public @NotNull BoneItemMapper toItemMapper() {
        return tags.isEmpty() ? BoneItemMapper.EMPTY : tags.stream().map(BoneTag::itemMapper).filter(Objects::nonNull).findFirst().orElse(BoneItemMapper.EMPTY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoneName boneName)) return false;
        return rawName.equals(boneName.rawName);
    }

    @Override
    public int hashCode() {
        return rawName.hashCode();
    }
}
