package kr.toxicity.model.api.nms;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;

public interface ModelDisplay {
    void frame(int frame);

    void spawn(@NotNull Player player);
    void remove(@NotNull Player player);
    void remove();
    void teleport(@NotNull Location location);
    void item(@NotNull ItemStack itemStack);
    void transform(@NotNull Transformation transformation);
}
