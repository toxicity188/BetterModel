package kr.toxicity.model.api.nms;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;

public interface ModelDisplay {
    void frame(int frame);

    void spawn(@NotNull PacketBundler bundler);
    void remove(@NotNull PacketBundler bundler);
    void teleport(@NotNull Location location);
    void item(@NotNull ItemStack itemStack);
    void transform(@NotNull Transformation transformation);
    void send(@NotNull PacketBundler bundler);
}
