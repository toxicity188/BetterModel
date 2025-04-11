package kr.toxicity.model.api.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Function;

/**
 * ItemStack with offset and scale
 * @see ItemStack
 * @param offset offset (x, y, z)
 * @param scale scale (x, y, z)
 * @param itemStack item
 */
public record TransformedItemStack(@NotNull Vector3f offset, @NotNull Vector3f scale, @NotNull ItemStack itemStack) {

    /**
     * Creates transformed item
     * @param itemStack item
     * @return transformed item
     */
    public static @NotNull TransformedItemStack of(@NotNull ItemStack itemStack) {
        return of(new Vector3f(), new Vector3f(1), itemStack);
    }


    /**
     * Creates transformed item
     * @param offset offset
     * @param scale scale
     * @param itemStack item
     * @return transformed item
     */
    public static @NotNull TransformedItemStack of(@NotNull Vector3f offset, @NotNull Vector3f scale, @NotNull ItemStack itemStack) {
        return new TransformedItemStack(offset, scale, itemStack);
    }

    /**
     * Gets transformed item as air
     * @return air item
     */
    public @NotNull TransformedItemStack asAir() {
        return of(offset, scale, new ItemStack(Material.AIR));
    }

    /**
     * Modify item
     * @param mapper mapper
     * @return modified item
     */
    public @NotNull TransformedItemStack modify(@NotNull Function<ItemStack, ItemStack> mapper) {
        return of(offset, scale, mapper.apply(itemStack.clone()));
    }

    /**
     * Checks this item is air
     * @return is air
     */
    public boolean isEmpty() {
        return switch (itemStack.getType()) {
            case AIR, CAVE_AIR, VOID_AIR -> true;
            default -> false;
        };
    }

    /**
     * Copy this item
     * @return copied item
     */
    public @NotNull TransformedItemStack copy() {
        return new TransformedItemStack(
                new Vector3f(offset),
                new Vector3f(scale),
                itemStack.clone()
        );
    }
}
