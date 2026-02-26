/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Bone tag registry
 */
public final class BoneTagRegistry {

    private final Map<String, BoneTag> byName = new HashMap<>();

    BoneTagRegistry() {
        for (BoneTags value : BoneTags.values()) {
            addTag(value);
        }
    }

    /**
     * Adds some tag to this registry
     * @param tag tag
     */
    public void addTag(@NotNull BoneTag tag) {
        BoneTag checkDuplicate;
        for (String s : tag.tags()) {
            if ((checkDuplicate = byName.put(s, tag)) != null) throw new RuntimeException("Duplicated tags: " + tag.name() + " between " + checkDuplicate.name());
        }
    }

    /**
     * Gets a bone tag by its name wrapped in an Optional.
     * @param tag tag name
     * @return bone tag
     * @since 1.15.2
     */
    public @NotNull Optional<BoneTag> byTagName(@NotNull String tag) {
        return Optional.ofNullable(byTagNameOrNull(tag));
    }

    /**
     * Gets a bone tag by its name.
     * @param tag tag name
     * @return bone tag or null
     * @since 2.1.0
     */
    public @Nullable BoneTag byTagNameOrNull(@NotNull String tag) {
        return byName.get(tag);
    }

    /**
     * Parses bone name by raw group name
     * @param rawName raw name
     * @return bone name
     */
    public @NotNull BoneName parse(@NotNull String rawName) {
        rawName = rawName.toLowerCase(Locale.ROOT);
        var tagArray = rawName.split("_");
        if (tagArray.length < 2) return new BoneName(Collections.emptySet(), rawName, rawName);
        var tagList = List.of(tagArray);
        var maxSize = tagList.size() - 1;
        var set = new HashSet<BoneTag>(maxSize);
        for (String s : tagList) {
            var tag = byTagNameOrNull(s);
            if (tag != null && set.size() < maxSize) {
                set.add(tag);
            } else return new BoneName(Collections.unmodifiableSet(set), String.join("_", tagList.subList(set.size(), tagList.size())), rawName);
        }
        return new BoneName(Collections.unmodifiableSet(set), String.join("_", tagList.subList(set.size(), tagList.size())), rawName);
    }
}
