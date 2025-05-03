package kr.toxicity.model.api.bone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

@Getter
@RequiredArgsConstructor
public enum BoneTag {
    NONE(new String[] {}),
    HEAD(new String[] { "h", "hi" }),
    HITBOX(new String[] { "b", "ob" }),
    SEAT(new String[] { "p" }),
    SUB_SEAT(new String[] { "sp" }),
    PLAYER_HEAD(new String[] { "ph" }),
    PLAYER_RIGHT_ARM(new String[] { "pra" }),
    PLAYER_RIGHT_FOREARM(new String[] { "prfa" }),
    PLAYER_LEFT_ARM(new String[] { "pla" }),
    PLAYER_LEFT_FOREARM(new String[] { "plfa" }),
    PLAYER_HIP(new String[] { "phip" }),
    PLAYER_WAIST(new String[] { "pw" }),
    PLAYER_CHEST(new String[] { "pc" }),
    PLAYER_RIGHT_LEG(new String[] { "prl" }),
    PLAYER_RIGHT_FORELEG(new String[] { "prfl" }),
    PLAYER_LEFT_LEG(new String[] { "pll" }),
    PLAYER_LEFT_FORELEG(new String[] { "plfl" })
    ;

    public static final Pattern TAG_PATTERN = Pattern.compile("^(?<tag>[a-zA-Z]+)_(?<name>(\\w|\\W)+)$");
    public static final Pattern TAGS_PATTERN = Pattern.compile("^(?<tags>[a-zA-Z]+(?:_[a-zA-Z]+)*)_(?<name>.*)$");
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

    public static @NotNull BoneName[] parseAll(@NotNull String rawName) {
        var matcher = TAGS_PATTERN.matcher(rawName);
        if (matcher.find()) {
            var name = matcher.group("name");
            return Arrays.stream(matcher.group("tags").split("_"))
                    .flatMap(tag -> BoneTag.byTagName(tag)
                            .stream()
                            .map(t -> new BoneName(t, name)))
                    .toArray(BoneName[]::new);
        }
        return new BoneName[0];
    }

    private final String[] tag;
}
