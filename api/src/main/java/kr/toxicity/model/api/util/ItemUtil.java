package kr.toxicity.model.api.util;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Item util
 */
public final class ItemUtil {
    /**
     * No initializer
     */
    private ItemUtil() {
        throw new RuntimeException();
    }

    /**
     * Checks this item should not be rendered by client
     * @param itemStack item
     * @return is air
     */
    public static boolean isEmpty(@NotNull ItemStack itemStack) {
        return itemStack.getType().isAir() || itemStack.getAmount() <= 0;
    }
}
