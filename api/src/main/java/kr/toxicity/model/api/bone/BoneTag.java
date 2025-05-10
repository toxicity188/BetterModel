package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BoneTag {
    @NotNull String name();
    @Nullable BoneItemMapper itemMapper();
    @NotNull String[] tags();
}
