/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static kr.toxicity.model.api.util.CollectionUtil.newAddressingMap;

/**
 * Bone tag registry
 */
public final class BoneTagRegistry {

    private static final String TAG_SPLITTER = "_";
    private final Object2ObjectMap<String, BoneTag> byName = newAddressingMap();

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
        var tagArray = rawName.split(TAG_SPLITTER);
        if (tagArray.length < 2) return new BoneName(ObjectSets.emptySet(), rawName, rawName);
        var tagList = List.of(tagArray);
        var maxSize = tagList.size() - 1;
        ObjectSet<BoneTag> set = maxSize <= 4 ? new ObjectArraySet<>(maxSize) : new ObjectOpenHashSet<>(maxSize);
        for (String s : tagList) {
            var tag = byTagNameOrNull(s);
            if (tag != null && set.size() < maxSize) set.add(tag);
            else return new BoneName(
                set.isEmpty() ? ObjectSets.emptySet() : ObjectSets.unmodifiable(set),
                set.isEmpty() ? rawName : String.join(TAG_SPLITTER, tagList.subList(set.size(), tagList.size())),
                rawName
            );
        }
        return new BoneName(
            ObjectSets.unmodifiable(set),
            String.join(TAG_SPLITTER, tagList.subList(set.size(), tagList.size())),
            rawName
        );
    }
}
