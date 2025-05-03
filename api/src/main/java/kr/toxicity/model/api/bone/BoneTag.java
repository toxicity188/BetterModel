package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.player.PlayerLimb;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

@Getter
@RequiredArgsConstructor
public enum BoneTag {
    HEAD(null, new String[] { "h", "hi" }),
    HITBOX(null, new String[] { "b", "ob" }),
    SEAT(null, new String[] { "p" }),
    SUB_SEAT(null, new String[] { "sp" }),

    PLAYER_HEAD(PlayerLimb.HEAD.getMapper(), new String[] { "ph" }),
    PLAYER_RIGHT_ARM(PlayerLimb.RIGHT_ARM.getMapper(), new String[] { "pra" }),
    PLAYER_RIGHT_FOREARM(PlayerLimb.RIGHT_FOREARM.getMapper(), new String[] { "prfa" }),
    PLAYER_LEFT_ARM(PlayerLimb.LEFT_ARM.getMapper(), new String[] { "pla" }),
    PLAYER_LEFT_FOREARM(PlayerLimb.LEFT_FOREARM.getMapper(), new String[] { "plfa" }),
    PLAYER_HIP(PlayerLimb.HIP.getMapper(), new String[] { "phip" }),
    PLAYER_WAIST(PlayerLimb.WAIST.getMapper(), new String[] { "pw" }),
    PLAYER_CHEST(PlayerLimb.CHEST.getMapper(), new String[] { "pc" }),
    PLAYER_RIGHT_LEG(PlayerLimb.RIGHT_LEG.getMapper(), new String[] { "prl" }),
    PLAYER_RIGHT_FORELEG(PlayerLimb.RIGHT_FORELEG.getMapper(), new String[] { "prfl" }),
    PLAYER_LEFT_LEG(PlayerLimb.LEFT_LEG.getMapper(), new String[] { "pll" }),
    PLAYER_LEFT_FORELEG(PlayerLimb.LEFT_FORELEG.getMapper(), new String[] { "plfl" }),
    PLAYER_LEFT_ITEM(PlayerLimb.LEFT_ITEM.getMapper(), new String[] { "pli" }),
    PLAYER_RIGHT_ITEM(PlayerLimb.RIGHT_ITEM.getMapper(), new String[] { "pri" })
    ;
    private static final Map<String, BoneTag> BY_NAME = new HashMap<>();

    static {
        BoneTag checkDuplicate;
        for (BoneTag value : values()) {
            for (String s : value.tag) {
                if ((checkDuplicate = BY_NAME.put(s, value)) != null) throw new RuntimeException("Duplicated tags: " + value.name() + " between " + checkDuplicate.name());
            }
        }
    }

    public static @NotNull Optional<BoneTag> byTagName(@NotNull String tag) {
        return Optional.ofNullable(BY_NAME.get(tag));
    }

    public static @NotNull BoneName parse(@NotNull String rawName) {
        var tagArray = Arrays.stream(rawName.split("_")).toList();
        if (tagArray.size() < 2) return new BoneName(Collections.emptySet(), rawName);
        var set = EnumSet.noneOf(BoneTag.class);
        for (String s : tagArray) {
            var tag = byTagName(s).orElse(null);
            if (tag != null && set.size() < tagArray.size()) {
                set.add(tag);
            } else return new BoneName(set, String.join("_", tagArray.subList(set.size(), tagArray.size())));
        }
        return new BoneName(set, String.join("_", tagArray.subList(set.size(), tagArray.size())));
    }

    @Nullable
    private final BoneItemMapper mapper;
    @NotNull
    private final String[] tag;
}
