package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

public record BoneName(@NotNull Set<BoneTag> tags, @NotNull String name) {
    public boolean tagged(@NotNull BoneTag... tags) {
        for (BoneTag boneTag : tags) {
            if (this.tags.contains(boneTag)) return true;
        }
        return false;
    }

    public @NotNull BoneItemMapper toItemMapper() {
        return tags.isEmpty() ? BoneItemMapper.EMPTY : tags.stream().map(BoneTag::itemMapper).filter(Objects::nonNull).findFirst().orElse(BoneItemMapper.EMPTY);
    }
}
