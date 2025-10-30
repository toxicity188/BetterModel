/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.util.TransformedItemStack;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Item-mapper of bone
 */
public interface BoneItemMapper extends BiFunction<RenderSource<?>, TransformedItemStack, TransformedItemStack> {

    @Override
    @NotNull TransformedItemStack apply(@NotNull RenderSource<?> renderSource, @NotNull TransformedItemStack transformedItemStack);

    /**
     * Empty
     */
    BoneItemMapper EMPTY = new BoneItemMapper() {
        @NotNull
        @Override
        public ItemDisplay.ItemDisplayTransform transform() {
            return ItemDisplay.ItemDisplayTransform.FIXED;
        }

        @Override
        @NotNull
        public TransformedItemStack apply(@NotNull RenderSource<?> source, @NotNull TransformedItemStack transformedItemStack) {
            return transformedItemStack;
        }
    };

    /**
     * Mapped if a render source is player
     * @param transform transformation
     * @param mapper mapper
     * @return bone item mapper
     */
    static @NotNull BoneItemMapper player(@NotNull ItemDisplay.ItemDisplayTransform transform, @NotNull Function<Player, TransformedItemStack> mapper) {
        return new BoneItemMapper() {

            private static final TransformedItemStack AIR = TransformedItemStack.empty();

            @NotNull
            @Override
            public ItemDisplay.ItemDisplayTransform transform() {
                return transform;
            }

            @Override
            public @NotNull TransformedItemStack apply(@NotNull RenderSource renderSource, @NotNull TransformedItemStack transformedItemStack) {
                if (renderSource instanceof RenderSource.BasePlayer(Player player)) {
                    var get = mapper.apply(player);
                    return get == null ? AIR : get;
                }
                return transformedItemStack;
            }
        };
    }

    /**
     * Mapped if a render source is entity
     * @param transform transformation
     * @param mapper mapper
     * @return bone item mapper
     */
    static @NotNull BoneItemMapper entity(@NotNull ItemDisplay.ItemDisplayTransform transform, @NotNull Function<BaseEntity, TransformedItemStack> mapper) {
        return new BoneItemMapper() {

            private static final TransformedItemStack AIR = TransformedItemStack.empty();

            @NotNull
            @Override
            public ItemDisplay.ItemDisplayTransform transform() {
                return transform;
            }

            @Override
            public @NotNull TransformedItemStack apply(@NotNull RenderSource renderSource, @NotNull TransformedItemStack transformedItemStack) {
                if (renderSource instanceof RenderSource.Entity entity) {
                    var get = mapper.apply(entity.entity());
                    return get == null ? AIR : get;
                }
                return transformedItemStack;
            }
        };
    }

    /**
     * Gets this mapper's display is fixed
     * @return fixed
     */
    default boolean fixed() {
        return transform() == ItemDisplay.ItemDisplayTransform.FIXED;
    }

    /**
     * Gets item display transformation
     * @return transformation
     */
    @NotNull ItemDisplay.ItemDisplayTransform transform();
}
