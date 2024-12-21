package kr.toxicity.model.api.nms;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface NMS {
    @NotNull ModelDisplay create(@NotNull Location location);
    @NotNull PlayerChannelHandler inject(@NotNull Player player);
    @NotNull PacketBundler createBundler();
    @NotNull ItemStack tint(@NotNull ItemStack itemStack, boolean toggle);
}
