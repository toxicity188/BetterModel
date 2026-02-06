/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * A tag of bone
 */
public interface BoneTag {

    /**
     * The default registry for bone tags.
     * @since 2.0.1
     */
    BoneTagRegistry REGISTRY = new BoneTagRegistry();

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
     * @since 2.0.1
     * @return tags
     */
    @NotNull @Unmodifiable
    List<String> tags();
}
