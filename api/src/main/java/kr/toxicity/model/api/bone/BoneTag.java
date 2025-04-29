package kr.toxicity.model.api.bone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Getter
@RequiredArgsConstructor
public enum BoneTag {
    NONE(new String[] {}),
    HEAD(new String[] { "h", "hi" }),
    HITBOX(new String[] { "b", "ob" }),
    SEAT(new String[] { "p" }),
    SUB_SEAT(new String[] { "sp" })
    ;

    public static final Pattern TAG_PATTERN = Pattern.compile("^(?<tag>[a-zA-Z]+)_(?<name>(\\w|\\W)+)$");
    private static final Map<String, BoneTag> BY_NAME = new HashMap<>();

    static {
        BoneTag checkDuplicate;
        for (BoneTag value : values()) {
            for (String s : value.tag) {
                if ((checkDuplicate = BY_NAME.put(s, value)) != null) throw new RuntimeException("Duplicated tag: " + value.name() + " between " + checkDuplicate.name());
            }
        }
    }

    public static @NotNull Optional<BoneTag> byTagName(@NotNull String tag) {
        return Optional.ofNullable(BY_NAME.get(tag));
    }

    public static @NotNull BoneName parse(@NotNull String rawName) {
        var matcher = TAG_PATTERN.matcher(rawName);
        if (matcher.find()) {
            return byTagName(matcher.group("tag"))
                    .map(t -> new BoneName(t, matcher.group("name")))
                    .orElseGet(() -> new BoneName(NONE, rawName));
        }
        return new BoneName(NONE, rawName);
    }

    private final String[] tag;
}
