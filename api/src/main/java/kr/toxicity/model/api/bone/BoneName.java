package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

/**
 * A tagged name of some bone
 * @param tags tags
 * @param name name
 */
public record BoneName(@NotNull Set<BoneTag> tags, @NotNull String name, @NotNull String rawName) {

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
}
