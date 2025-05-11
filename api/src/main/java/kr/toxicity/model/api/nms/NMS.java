package kr.toxicity.model.api.nms;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox;
import kr.toxicity.model.api.mount.MountController;
import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
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
    default @NotNull PacketBundler createBundler() {
        return createBundler(true);
    }

    /**
     * Creates packet bundler
     * @param useEntityTrack use entity track
     * @return packet bundler
     */
    @NotNull PacketBundler createBundler(boolean useEntityTrack);

    /**
     * Applies tint to some item
     * @param itemStack item
     * @param rgb hex rgb value
     * @return new item
     */
    @NotNull ItemStack tint(@NotNull ItemStack itemStack, int rgb);

    /**
     * Adds a mount packet for this tracker to bundler
     * @param tracker tracker
     * @param bundler bundler
     */
    void mount(@NotNull EntityTracker tracker, @NotNull PacketBundler bundler);

    /**
     * Sends a hide packet for some entity to some player
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
    @NotNull EntityAdapter adapt(@NotNull Entity entity);

    /**
     * Checks current thread is sync or not
     * @return sync or not
     */
    boolean isSync();

    /**
     * Gets game profile from player
     * @param player player
     * @return game profile
     */
    @NotNull GameProfile profile(@NotNull Player player);

    /**
     * Checks this profile skin is slim
     * @param profile profile
     * @return slime or not
     */
    boolean isSlim(@NotNull GameProfile profile);

    /**
     * Creates player head from game profile
     * @param profile profile
     * @return player head item
     */
    @NotNull ItemStack createPlayerHead(@NotNull GameProfile profile);
}
