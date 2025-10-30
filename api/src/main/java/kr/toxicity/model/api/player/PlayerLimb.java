/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.player;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.BoneItemMapper;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.skin.SkinData;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.TransformedItemStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Function;

/**
 * Player limb data
 */
@RequiredArgsConstructor
@Getter
public enum PlayerLimb {
    /**
     * Head
     */
    HEAD(
            position(0),
            scale(7.4688F, 0.5F),
            scale(7.4688F, 0.5F),
            offset(0, 7.5F, 0, 0.5F),
            offset(0, 7.5F, 0, 0.5F),
            SkinData::head,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * Right arm
     */
    RIGHT_ARM(
            position(1),
            scale(3.7188F,5.5938F,3.7188F, 0.25F),
            scale(2.7891F,5.5938F,3.7188F, 0.25F),
            offset(-0.625F, 1.5F, 0, 0.25F),
            offset(-0.043F, 1.5F, 0, 0.25F),
            SkinData::rightArm,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * Right forearm
     */
    RIGHT_FOREARM(
            position(2),
            scale(3.7188F,5.5938F,3.7188F, 0.25F),
            scale(2.7891F,5.5938F,3.7188F, 0.25F),
            offset(-0.625F, 1.5F, 0, 0.25F),
            offset(-0.043F, 1.5F, 0, 0.25F),
            SkinData::rightForeArm,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * Left arm
     */
    LEFT_ARM(
            position(3),
            scale(3.7188F,5.5938F,3.7188F, 0.25F),
            scale(2.7891F,5.5938F,3.7188F, 0.25F),
            offset(0.625F, 1.5F, 0, 0.25F),
            offset(0.043F, 1.5F, 0, 0.25F),
            SkinData::leftArm,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * Left forearm
     */
    LEFT_FOREARM(
            position(4), 
            scale(3.7188F,5.5938F,3.7188F, 0.25F),
            scale(2.7891F,5.5938F,3.7188F, 0.25F),
            offset(0.625F, 1.5F, 0, 0.25F), 
            offset(0.043F, 1.5F, 0, 0.25F),
            SkinData::leftForeArm,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * Hip
     */
    HIP(
            position(5), 
            scale(7.4688F,3.7188F,3.7188F, 0.25F), 
            scale(7.4688F,3.7188F,3.7188F, 0.25F),
            offset(0, 5.75F, 0, 0.25F),
            offset(0, 5.75F, 0, 0.25F),
            SkinData::hip,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * Waist
     */
    WAIST(
            position(6), 
            scale(7.4688F,3.7188F,3.7188F, 0.25F), 
            scale(7.4688F,3.7188F,3.7188F, 0.25F),
            offset(0, 5.75F, 0, 0.25F),
            offset(0, 5.75F, 0, 0.25F),
            SkinData::waist,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * Chest
     */
    CHEST(
            position(7), 
            scale(7.4688F,3.7188F,3.7188F, 0.25F), 
            scale(7.4688F,3.7188F,3.7188F, 0.25F),
            offset(0, 5.75F, 0, 0.25F),
            offset(0, 5.75F, 0, 0.25F),
            SkinData::chest,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * Right leg
     */
    RIGHT_LEG(
            position(8), 
            scale(3.7188F,5.5938F,3.7188F, 0.25F), 
            scale(3.7188F,5.5938F,3.7188F, 0.25F),
            offset(0, 1.12F, 0, 0.25F),
            offset(0, 1.12F, 0, 0.25F),
            SkinData::rightLeg,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * Right foreleg
     */
    RIGHT_FORELEG(
            position(9), 
            scale(3.7188F,5.5938F,3.7188F, 0.25F), 
            scale(3.7188F,5.5938F,3.7188F, 0.25F),
            offset(0, 1.12F, 0, 0.25F),
            offset(0, 1.12F, 0, 0.25F),
            SkinData::rightForeLeg,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * LEft leg
     */
    LEFT_LEG(
            position(10), 
            scale(3.7188F,5.5938F,3.7188F, 0.25F), 
            scale(3.7188F,5.5938F,3.7188F, 0.25F),
            offset(0, 1.12F, 0, 0.25F),
            offset(0, 1.12F, 0, 0.25F),
            SkinData::leftLeg,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    /**
     * Left foreleg
     */
    LEFT_FORELEG(
            position(11), 
            scale(3.7188F,5.5938F,3.7188F, 0.25F), 
            scale(3.7188F,5.5938F,3.7188F, 0.25F),
            offset(0, 1.12F, 0, 0.25F),
            offset(0, 1.12F, 0, 0.25F),
            SkinData::leftForeLeg,
            ItemDisplay.ItemDisplayTransform.FIXED
    ),
    ;

    private static @NotNull Vector3f position(int mul) {
        return new Vector3f(0, -512, 0).mul(mul);
    }

    private static @NotNull Vector3f scale(float scale, float inflate) {
        return scale(scale, scale, scale, inflate);
    }

    private static @NotNull Vector3f scale(float x, float y, float z, float inflate) {
        return new Vector3f(x, y, z).div(8).add(new Vector3f(inflate).div(8));
    }

    private static @NotNull Vector3f offset(float x, float y, float z, float inflate) {
        return new Vector3f(0, -0.25F, 0).add(new Vector3f(x, y, z).div(MathUtil.MODEL_TO_BLOCK_MULTIPLIER)).add(new Vector3f(0, inflate, 0).div(32));
    }

    private final @NotNull Vector3f position;
    private final @NotNull Vector3f scale;
    private final @NotNull Vector3f slimScale;
    private final @NotNull Vector3f offset;
    private final @NotNull Vector3f slimOffset;
    private final @NotNull Function<SkinData, TransformedItemStack> skinMapper;
    private final @NotNull ItemDisplay.ItemDisplayTransform transform;

    @Getter
    private final @NotNull LimbItemMapper itemMapper = new LimbItemMapper(this::createItem);

    /**
     * Generates transformed item from player
     * @param profiled target player
     * @return item
     */
    public @NotNull TransformedItemStack createItem(@NotNull RenderSource.Profiled profiled) {
        var profile = profiled.profile();
        var manager = BetterModel.plugin().skinManager();
        if (manager.supported()) {
            return skinMapper.apply(manager.getOrRequest(profile));
        }
        var isSlim = profiled.slim();
        return TransformedItemStack.of(position, isSlim ? slimOffset : offset, isSlim ? slimScale : scale, BetterModel.nms().createPlayerHead(profiled.profile()));
    }

    /**
     * Limb item mapper
     */
    @RequiredArgsConstructor
    public class LimbItemMapper implements BoneItemMapper {

        private final Function<RenderSource.Profiled, TransformedItemStack> playerMapper;

        @NotNull
        @Override
        public ItemDisplay.ItemDisplayTransform transform() {
            return transform;
        }

        @Override
        @NotNull
        public TransformedItemStack apply(@NotNull RenderSource<?> source, @NotNull TransformedItemStack transformedItemStack) {
            if (source instanceof RenderSource.Profiled profiled) {
                return playerMapper.apply(profiled);
            }
            return transformedItemStack;
        }
    }
}