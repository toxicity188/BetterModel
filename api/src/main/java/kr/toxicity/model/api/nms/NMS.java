package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.data.blueprint.NamedBoundingBox;
import kr.toxicity.model.api.mount.MountController;
import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NMS {
    @NotNull ModelDisplay create(@NotNull Location location);
    @NotNull PlayerChannelHandler inject(@NotNull Player player);
    @NotNull PacketBundler createBundler();
    @NotNull ItemStack tint(@NotNull ItemStack itemStack, int rgb);
    void mount(@NotNull EntityTracker tracker, @NotNull PacketBundler bundler);
    void hide(@NotNull Player player, @NotNull Entity entity);
    @Nullable HitBox createHitBox(@NotNull EntityAdapter entity, @NotNull HitBoxSource supplier, @NotNull NamedBoundingBox namedBoundingBox, @NotNull MountController controller, @NotNull HitBoxListener source);
    @NotNull NMSVersion version();
    @NotNull EntityAdapter adapt(@NotNull LivingEntity entity);
}
