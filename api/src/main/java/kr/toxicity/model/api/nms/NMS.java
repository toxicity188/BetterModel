package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface NMS {
    @NotNull ModelDisplay create(@NotNull Location location);
    @NotNull PlayerChannelHandler inject(@NotNull Player player);
    @NotNull PacketBundler createBundler();
    @NotNull ItemStack tint(@NotNull ItemStack itemStack, boolean toggle);
    void mount(@NotNull EntityTracker tracker, @NotNull PacketBundler bundler);
    void boundingBox(@NotNull Entity entity, @NotNull BoundingBox box);
    @NotNull NMSVersion version();
    @NotNull Vector3f passengerPosition(@NotNull Entity entity);
}
