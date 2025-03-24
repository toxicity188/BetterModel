package kr.toxicity.model.api.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record ScaledItemStack(@NotNull Vector3f scale, @NotNull ItemStack itemStack) {

    public static final @NotNull ScaledItemStack EMPTY = of(new ItemStack(Material.AIR));

    public static @NotNull ScaledItemStack of(@NotNull ItemStack itemStack) {
        return of(new Vector3f(1), itemStack);
    }
    public static @NotNull ScaledItemStack of(@NotNull Vector3f scale, @NotNull ItemStack itemStack) {
        return new ScaledItemStack(scale, itemStack);
    }

    public @NotNull ScaledItemStack copy() {
        return new ScaledItemStack(
                new Vector3f(scale),
                itemStack.clone()
        );
    }
}
