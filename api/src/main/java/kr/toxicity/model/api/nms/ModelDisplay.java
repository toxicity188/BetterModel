package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.tracker.ModelRotation;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;

public interface ModelDisplay {
    void rotate(@NotNull ModelRotation rotation, @NotNull PacketBundler bundler);
    void sync(@NotNull EntityAdapter entity);
    void frame(int frame);
    void display(@NotNull ItemDisplay.ItemDisplayTransform transform);
    void spawn(@NotNull PacketBundler bundler);
    void remove(@NotNull PacketBundler bundler);
    void teleport(@NotNull Location location, @NotNull PacketBundler bundler);
    void item(@NotNull ItemStack itemStack);
    void transform(@NotNull Transformation transformation);
    void send(@NotNull PacketBundler bundler);
}
