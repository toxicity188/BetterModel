package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.player.PlayerLimb;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builtin tags
 */
public enum BoneTags implements BoneTag {
    /**
     * Follows entity's head rotation
     */
    HEAD(new String[] { "h" }),
    /**
     * Follows entity's head rotation
     */
    HEAD_WITH_CHILDREN(new String[] { "hi" }),
    /**
     * Creates a hitbox following this bone
     */
    HITBOX(new String[] { "b", "ob" }),
    /**
     * It can be used as a seat
     */
    SEAT(new String[] { "p" }),
    /**
     * It can be used as a seat but not controllable
     */
    SUB_SEAT(new String[] { "sp" }),
    /**
     * Nametag
     */
    TAG(new String[] { "tag" }),
    /**
     * Player head
     */
    PLAYER_HEAD(PlayerLimb.HEAD.getItemMapper(), new String[] { "ph" }),
    /**
     * Player right arm
     */
    PLAYER_RIGHT_ARM(PlayerLimb.RIGHT_ARM.getItemMapper(), new String[] { "pra" }),
    /**
     * Player right forearm
     */
    PLAYER_RIGHT_FOREARM(PlayerLimb.RIGHT_FOREARM.getItemMapper(), new String[] { "prfa" }),
    /**
     * Player left arm
     */
    PLAYER_LEFT_ARM(PlayerLimb.LEFT_ARM.getItemMapper(), new String[] { "pla" }),
    /**
     * Player left forearm
     */
    PLAYER_LEFT_FOREARM(PlayerLimb.LEFT_FOREARM.getItemMapper(), new String[] { "plfa" }),
    /**
     * Player left hip
     */
    PLAYER_HIP(PlayerLimb.HIP.getItemMapper(), new String[] { "phip" }),
    /**
     * Player left waist
     */
    PLAYER_WAIST(PlayerLimb.WAIST.getItemMapper(), new String[] { "pw" }),
    /**
     * Player left chest
     */
    PLAYER_CHEST(PlayerLimb.CHEST.getItemMapper(), new String[] { "pc" }),
    /**
     * Player right leg
     */
    PLAYER_RIGHT_LEG(PlayerLimb.RIGHT_LEG.getItemMapper(), new String[] { "prl" }),
    /**
     * Player right foreleg
     */
    PLAYER_RIGHT_FORELEG(PlayerLimb.RIGHT_FORELEG.getItemMapper(), new String[] { "prfl" }),
    /**
     * Player left leg
     */
    PLAYER_LEFT_LEG(PlayerLimb.LEFT_LEG.getItemMapper(), new String[] { "pll" }),
    /**
     * Player left foreleg
     */
    PLAYER_LEFT_FORELEG(PlayerLimb.LEFT_FORELEG.getItemMapper(), new String[] { "plfl" }),
    /**
     * Player's item in left hand
     */
    PLAYER_LEFT_ITEM(PlayerLimb.LEFT_ITEM.getItemMapper(), new String[] { "pli" }),
    /**
     * Player's item in right hand
     */
    PLAYER_RIGHT_ITEM(PlayerLimb.RIGHT_ITEM.getItemMapper(), new String[] { "pri" })
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
