package kr.toxicity.model.api.util;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ItemUtil {
    private ItemUtil() {
        throw new RuntimeException();
    }

    public static boolean isEmpty(@NotNull ItemStack itemStack) {
        return itemStack.getType().isAir() || itemStack.getAmount() <= 0;
    }
}
