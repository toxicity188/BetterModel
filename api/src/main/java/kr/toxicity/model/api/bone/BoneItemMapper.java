package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.util.TransformedItemStack;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public interface BoneItemMapper extends BiFunction<RenderSource, TransformedItemStack, TransformedItemStack> {

    BoneItemMapper EMPTY = new BoneItemMapper() {
        @NotNull
        @Override
        public ItemDisplay.ItemDisplayTransform transform() {
            return ItemDisplay.ItemDisplayTransform.FIXED;
        }

        @Override
        @NotNull
        public TransformedItemStack apply(@NotNull RenderSource source, @NotNull TransformedItemStack transformedItemStack) {
            return transformedItemStack;
        }
    };

    @NotNull ItemDisplay.ItemDisplayTransform transform();
}
