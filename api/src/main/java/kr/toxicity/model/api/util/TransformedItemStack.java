package kr.toxicity.model.api.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record TransformedItemStack(@NotNull Vector3f offset, @NotNull Vector3f scale, @NotNull ItemStack itemStack) {

    public static final @NotNull TransformedItemStack EMPTY = of(new ItemStack(Material.AIR));

    public static @NotNull TransformedItemStack of(@NotNull ItemStack itemStack) {
        return of(new Vector3f(), new Vector3f(1), itemStack);
    }
    public static @NotNull TransformedItemStack of(@NotNull Vector3f offset, @NotNull Vector3f scale, @NotNull ItemStack itemStack) {
        return new TransformedItemStack(offset, scale, itemStack);
    }

    public boolean isEmpty() {
        return switch (itemStack.getType()) {
            case AIR, CAVE_AIR, VOID_AIR -> true;
            default -> false;
        };
    }

    public @NotNull TransformedItemStack copy() {
        return new TransformedItemStack(
                new Vector3f(offset),
                new Vector3f(scale),
                itemStack.clone()
        );
    }
}
