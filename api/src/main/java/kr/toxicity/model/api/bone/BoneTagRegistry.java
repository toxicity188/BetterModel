package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class BoneTagRegistry {
    private static final Map<String, BoneTag> BY_NAME = new HashMap<>();

    private BoneTagRegistry() {
        throw new RuntimeException();
    }

    public static void addTag(@NotNull BoneTag tag) {
        BoneTag checkDuplicate;
        for (String s : tag.tags()) {
            if ((checkDuplicate = BY_NAME.put(s, tag)) != null) throw new RuntimeException("Duplicated tags: " + tag.name() + " between " + checkDuplicate.name());
        }
    }


    public static @NotNull Optional<BoneTag> byTagName(@NotNull String tag) {
        return Optional.ofNullable(BY_NAME.get(tag));
    }

    public static @NotNull BoneName parse(@NotNull String rawName) {
        var tagArray = Arrays.stream(rawName.split("_")).toList();
        if (tagArray.size() < 2) return new BoneName(Collections.emptySet(), rawName);
        var set = new HashSet<BoneTag>();
        for (String s : tagArray) {
            var tag = byTagName(s).orElse(null);
            if (tag != null && set.size() < tagArray.size()) {
                set.add(tag);
            } else return new BoneName(set, String.join("_", tagArray.subList(set.size(), tagArray.size())));
        }
        return new BoneName(set, String.join("_", tagArray.subList(set.size(), tagArray.size())));
    }
}
