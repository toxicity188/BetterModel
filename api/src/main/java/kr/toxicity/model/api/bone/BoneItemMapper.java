/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.platform.PlatformItemTransform;
import kr.toxicity.model.api.platform.PlatformPlayer;
import kr.toxicity.model.api.util.TransformedItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Item-mapper of bone
 */
public interface BoneItemMapper extends BiFunction<BoneRenderContext, TransformedItemStack, TransformedItemStack> {

    @Override
    @NotNull TransformedItemStack apply(@NotNull BoneRenderContext context, @NotNull TransformedItemStack transformedItemStack);

    /**
     * Empty
     */
    BoneItemMapper EMPTY = new BoneItemMapper() {
        @NotNull
        @Override
        public PlatformItemTransform transform() {
            return PlatformItemTransform.FIXED;
        }

        @Override
        @NotNull
        public TransformedItemStack apply(@NotNull BoneRenderContext context, @NotNull TransformedItemStack transformedItemStack) {
            return transformedItemStack;
        }
    };

    /**
     * Mapped if a render source is player
     * @param transform transformation
     * @param mapper mapper
     * @return bone item mapper
     */
    static @NotNull BoneItemMapper player(@NotNull PlatformItemTransform transform, @NotNull Function<PlatformPlayer, TransformedItemStack> mapper) {
        return new BoneItemMapper() {

            private static final TransformedItemStack AIR = TransformedItemStack.empty();

            @NotNull
            @Override
            public PlatformItemTransform transform() {
                return transform;
            }

            @Override
            public @NotNull TransformedItemStack apply(@NotNull BoneRenderContext context, @NotNull TransformedItemStack transformedItemStack) {
                if (context.source() instanceof RenderSource.BasePlayer(PlatformPlayer player)) {
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
    static @NotNull BoneItemMapper entity(@NotNull PlatformItemTransform transform, @NotNull Function<BaseEntity, TransformedItemStack> mapper) {
        return new BoneItemMapper() {

            private static final TransformedItemStack AIR = TransformedItemStack.empty();

            @NotNull
            @Override
            public PlatformItemTransform transform() {
                return transform;
            }

            @Override
            public @NotNull TransformedItemStack apply(@NotNull BoneRenderContext context, @NotNull TransformedItemStack transformedItemStack) {
                if (context.source() instanceof RenderSource.Entity entity) {
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
        return transform() == PlatformItemTransform.FIXED;
    }

    /**
     * Gets item display transformation
     * @return transformation
     */
    @NotNull PlatformItemTransform transform();
}
