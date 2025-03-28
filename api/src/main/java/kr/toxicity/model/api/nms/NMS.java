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

/**
 * A vanilla code handler of Minecraft (NMS)
 */
public interface NMS {
    /**
     * Creates model display
     * @param location start location
     * @return model display
     */
    @NotNull ModelDisplay create(@NotNull Location location);

    /**
     * Injects netty channel handler to player
     * @param player player
     * @return channel handler
     */
    @NotNull PlayerChannelHandler inject(@NotNull Player player);

    /**
     * Creates packet bundler
     * @return packet bundler
     */
    @NotNull PacketBundler createBundler();

    /**
     * Applies tint to some item
     * @param itemStack item
     * @param rgb hex rgb value
     * @return new item
     */
    @NotNull ItemStack tint(@NotNull ItemStack itemStack, int rgb);

    /**
     * Adds mount packet for this tracker to bundler
     * @param tracker tracker
     * @param bundler bundler
     */
    void mount(@NotNull EntityTracker tracker, @NotNull PacketBundler bundler);

    /**
     * Sends hide packet for some entity to some player
     * @param player player
     * @param entity entity
     */
    void hide(@NotNull Player player, @NotNull Entity entity);

    /**
     * Creates delegator hit-box of target entity
     * @param entity target entity
     * @param supplier transformation supplier
     * @param namedBoundingBox bounding box
     * @param controller mount controller
     * @param listener hitbox listener
     * @return hit-box
     */
    @Nullable HitBox createHitBox(@NotNull EntityAdapter entity, @NotNull HitBoxSource supplier, @NotNull NamedBoundingBox namedBoundingBox, @NotNull MountController controller, @NotNull HitBoxListener listener);

    /**
     * Gets Spigot-mapped version of Minecraft vanilla code.
     * @return version
     */
    @NotNull NMSVersion version();

    /**
     * Gets adapted entity value getter of target entity for Folia
     * @param entity entity
     * @return adapter
     */
    @NotNull EntityAdapter adapt(@NotNull LivingEntity entity);
}
