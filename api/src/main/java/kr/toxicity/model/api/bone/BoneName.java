package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;

public record BoneName(@NotNull BoneTag tag, @NotNull String name) {
    public boolean tagged(@NotNull BoneTag... tags) {
        for (BoneTag boneTag : tags) {
            if (boneTag == tag) return true;
        }
        return false;
    }
}
