package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A tag of bone
 */
public interface BoneTag {
    /**
     * Gets tag name
     * @return tag name
     */
    @NotNull String name();

    /**
     * Gets an item mapper
     * @return item mapper
     */
    @Nullable BoneItemMapper itemMapper();

    /**
     * Gets a tag list like 'h', 'hi', 'b'
     * @return tags
     */
    @NotNull String[] tags();
}
