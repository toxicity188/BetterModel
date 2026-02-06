/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;

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
     * Gets bone tag by tag name
     * @param tag tag name
     * @return optional tag
     */
    public @NotNull Optional<BoneTag> byTagName(@NotNull String tag) {
        return Optional.ofNullable(byName.get(tag));
    }

    /**
     * Parses bone name by raw group name
     * @param rawName raw name
     * @return bone name
     */
    public @NotNull BoneName parse(@NotNull String rawName) {
        rawName = rawName.toLowerCase(Locale.ROOT);
        var tagArray = List.of(rawName.split("_"));
        if (tagArray.size() < 2) return new BoneName(Collections.emptySet(), rawName, rawName);
        var maxSize = tagArray.size() - 1;
        var set = new HashSet<BoneTag>(maxSize);
        for (String s : tagArray) {
            var tag = byTagName(s).orElse(null);
            if (tag != null && set.size() < maxSize) {
                set.add(tag);
            } else return new BoneName(Collections.unmodifiableSet(set), String.join("_", tagArray.subList(set.size(), tagArray.size())), rawName);
        }
        return new BoneName(Collections.unmodifiableSet(set), String.join("_", tagArray.subList(set.size(), tagArray.size())), rawName);
    }
}
