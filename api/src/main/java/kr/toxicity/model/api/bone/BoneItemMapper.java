package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.util.TransformedItemStack;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

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

    static @NotNull BoneItemMapper player(@NotNull ItemDisplay.ItemDisplayTransform transform, @NotNull Function<Player, TransformedItemStack> mapper) {
        return new BoneItemMapper() {

            private static final TransformedItemStack AIR = TransformedItemStack.of(new ItemStack(Material.AIR));

            @NotNull
            @Override
            public ItemDisplay.ItemDisplayTransform transform() {
                return transform;
            }

            @Override
            public TransformedItemStack apply(RenderSource renderSource, TransformedItemStack transformedItemStack) {
                if (renderSource instanceof RenderSource.BasePlayer(Player player)) {
                    var get = mapper.apply(player);
                    return get == null ? AIR : get;
                }
                return transformedItemStack;
            }
        };
    }

    @NotNull ItemDisplay.ItemDisplayTransform transform();
}
