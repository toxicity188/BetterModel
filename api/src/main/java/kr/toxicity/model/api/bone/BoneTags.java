/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.nms.Profiled;
import kr.toxicity.model.api.platform.PlatformItemTransform;
import kr.toxicity.model.api.player.PlayerLimb;
import kr.toxicity.model.api.util.TransformedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Builtin tags
 */
public enum BoneTags implements BoneTag {
    /**
     * Follows entity's head rotation
     */
    HEAD("h"),
    /**
     * Follows entity's head rotation
     */
    HEAD_WITH_CHILDREN("hi"),
    /**
     * Creates a hitbox following this bone
     */
    HITBOX("b", "ob"),
    /**
     * It can be used as a seat
     */
    SEAT("p"),
    /**
     * It can be used as a seat but not controllable
     */
    SUB_SEAT("sp"),
    /**
     * Nametag
     */
    TAG("tag"),
    /**
     * Mob's nametag
     */
    MOB_TAG("mtag"),
    /**
     * Player's nametag
     */
    PLAYER_TAG("ptag"),
    /**
     * Entity's item in left hand
     */
    LEFT_ITEM(BoneItemMapper.entity(
        PlatformItemTransform.THIRDPERSON_LEFTHAND,
        BaseEntity::offHand
    ), "pli", "li"),
    /**
     * Entity's item in right hand
     */
    RIGHT_ITEM(BoneItemMapper.entity(
        PlatformItemTransform.THIRDPERSON_RIGHTHAND,
        BaseEntity::mainHand
    ), "pri", "ri"),
    /**
     * Player head
     */
    PLAYER_HEAD(PlayerLimb.HEAD.getItemMapper(), "ph"),
    /**
     * Player right arm
     */
    PLAYER_RIGHT_ARM(PlayerLimb.RIGHT_ARM.getItemMapper(), "pra"),
    /**
     * Player right forearm
     */
    PLAYER_RIGHT_FOREARM(PlayerLimb.RIGHT_FOREARM.getItemMapper(), "prfa"),
    /**
     * Player left arm
     */
    PLAYER_LEFT_ARM(PlayerLimb.LEFT_ARM.getItemMapper(), "pla"),
    /**
     * Player left forearm
     */
    PLAYER_LEFT_FOREARM(PlayerLimb.LEFT_FOREARM.getItemMapper(), "plfa"),
    /**
     * Player left hip
     */
    PLAYER_HIP(PlayerLimb.HIP.getItemMapper(), "phip"),
    /**
     * Player left waist
     */
    PLAYER_WAIST(PlayerLimb.WAIST.getItemMapper(), "pw"),
    /**
     * Player left chest
     */
    PLAYER_CHEST(PlayerLimb.CHEST.getItemMapper(), "pc"),
    /**
     * Player right leg
     */
    PLAYER_RIGHT_LEG(PlayerLimb.RIGHT_LEG.getItemMapper(), "prl"),
    /**
     * Player right foreleg
     */
    PLAYER_RIGHT_FORELEG(PlayerLimb.RIGHT_FORELEG.getItemMapper(), "prfl"),
    /**
     * Player left leg
     */
    PLAYER_LEFT_LEG(PlayerLimb.LEFT_LEG.getItemMapper(), "pll"),
    /**
     * Player left foreleg
     */
    PLAYER_LEFT_FORELEG(PlayerLimb.LEFT_FORELEG.getItemMapper(), "plfl"),
    /**
     * Cape
     */
    CAPE(new BoneItemMapper() {
        @Override
        public @NotNull TransformedItemStack apply(@NotNull BoneRenderContext context, @NotNull TransformedItemStack transformedItemStack) {
            TransformedItemStack cape = null;
            if (BetterModel.platform().skinManager().supported() && context.source() instanceof Profiled profiled && profiled.skinParts().isCapeEnabled()) {
                cape = context.skin().cape(profiled.armors());
            }
            return cape != null ? cape : TransformedItemStack.empty();
        }

        @Override
        public @NotNull PlatformItemTransform transform() {
            return PlatformItemTransform.FIXED;
        }
    }, "cape")
    ;

    BoneTags(@NotNull String... tags) {
        this(null, tags);
    }

    BoneTags(@Nullable BoneItemMapper itemMapper, @NotNull String... tags) {
        this.itemMapper = itemMapper;
        this.tags = List.of(tags);
    }

    @Nullable
    private final BoneItemMapper itemMapper;
    @NotNull
    private final List<String> tags;

    @Nullable
    @Override
    public BoneItemMapper itemMapper() {
        return itemMapper;
    }

    @NotNull
    @Unmodifiable
    @Override
    public List<String> tags() {
        return tags;
    }
}
