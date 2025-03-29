package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.tracker.ModelRotation;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;

/**
 * An item-display packet adapter
 */
public interface ModelDisplay {
    /**
     * Rotates this display
     * @param rotation rotation
     * @param bundler packet bundler
     */
    void rotate(@NotNull ModelRotation rotation, @NotNull PacketBundler bundler);

    /**
     * Syncs entity data of source entity
     * @param entity source
     */
    void sync(@NotNull EntityAdapter entity);

    /**
     * Sets transformation interpolation duration
     * @param frame frame time
     */
    void frame(int frame);

    /**
     * Sets item billboard
     * @param transform billboard
     */
    void display(@NotNull ItemDisplay.ItemDisplayTransform transform);

    /**
     * Spawns this display
     * @param bundler packet bundler
     */
    void spawn(@NotNull PacketBundler bundler);

    /**
     * Removes this display
     * @param bundler packet bundler
     */
    void remove(@NotNull PacketBundler bundler);

    /**
     * Teleports this display
     * @param location location
     * @param bundler packet bundler
     */
    void teleport(@NotNull Location location, @NotNull PacketBundler bundler);

    /**
     * Sets displayed item
     * @param itemStack item
     */
    void item(@NotNull ItemStack itemStack);

    /**
     * Transforms this display
     * @param transformation transformation
     */
    void transform(@NotNull Transformation transformation);

    /**
     * Sends all entity data
     * @param bundler packet bundler
     */
    void send(@NotNull PacketBundler bundler);

    /**
     * Sets brightness overrides
     * @param block block
     * @param sky sky
     */
    void brightness(int block, int sky);
}
