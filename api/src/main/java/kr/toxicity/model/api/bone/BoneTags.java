package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.player.PlayerLimb;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum BoneTags implements BoneTag {
    HEAD(new String[] { "h", "hi" }),
    HITBOX(new String[] { "b", "ob" }),
    SEAT(new String[] { "p" }),
    SUB_SEAT(new String[] { "sp" }),

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

    BoneTags(@NotNull String[] tags) {
        this(null, tags);
    }

    BoneTags(@Nullable BoneItemMapper itemMapper, @NotNull String[] tags) {
        this.itemMapper = itemMapper;
        this.tags = tags;
    }

    @Nullable
    private final BoneItemMapper itemMapper;
    @NotNull
    private final String[] tags;

    @Nullable
    @Override
    public BoneItemMapper itemMapper() {
        return itemMapper;
    }

    @NotNull
    @Override
    public String[] tags() {
        return tags;
    }
}
