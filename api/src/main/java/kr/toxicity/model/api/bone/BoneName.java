package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.player.PlayerLimb;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record BoneName(@NotNull BoneTag tag, @NotNull String name) {
    public boolean tagged(@NotNull BoneTag... tags) {
        for (BoneTag boneTag : tags) {
            if (boneTag == tag) return true;
        }
        return false;
    }

    public @Nullable PlayerLimb toLimb() {
        return PlayerLimb.getLimb(tag.name());
    }
}
