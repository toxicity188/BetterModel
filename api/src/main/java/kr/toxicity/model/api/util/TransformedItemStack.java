package kr.toxicity.model.api.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * ItemStack with offset and scale
 * @see ItemStack
 * @param offset offset (x, y, z)
 * @param scale scale (x, y, z)
 * @param itemStack item
 */
public record TransformedItemStack(@NotNull Vector3f offset, @NotNull Vector3f scale, @NotNull ItemStack itemStack) {

    /**
     * Air
     */
    public static final @NotNull TransformedItemStack EMPTY = of(new ItemStack(Material.AIR));

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
