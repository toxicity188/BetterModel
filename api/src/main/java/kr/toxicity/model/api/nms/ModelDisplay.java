package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.tracker.ModelRotation;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * An item-display packet adapter
 */
public interface ModelDisplay extends Identifiable {

    /**
     * Checks this display is invisible
     * @return invisible
     */
    boolean invisible();

    /**
     * Sets the invisibility of this display
     * @param invisible is invisible
     */
    void invisible(boolean invisible);

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
     * Sets pos-rot movement interpolation duration
     * @param duration duration
     */
    void moveDuration(int duration);

    /**
     * Sets item billboard
     * @param transform billboard
     */
    void display(@NotNull ItemDisplay.ItemDisplayTransform transform);

    /**
     * Spawns this display
     * @param bundler packet bundler
     */
    default void spawn(@NotNull PacketBundler bundler) {
        spawn(!invisible(), bundler);
    }

    /**
     * Spawns this display
     * @param showItem show item
     * @param bundler packet bundler
     */
    void spawn(boolean showItem, @NotNull PacketBundler bundler);

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
     * @param duration duration
     * @param position position
     * @param scale scale
     * @param rotation rotation
     * @param bundler bundler
     */
    void transform(int duration, @NotNull Vector3f position, @NotNull Vector3f scale, @NotNull Quaternionf rotation, @NotNull PacketBundler bundler);

    /**
     * Sends transformation
     * @param bundler packet bundler
     */
    void sendTransformation(@NotNull PacketBundler bundler);

    /**
     * Sends entity data
     * @param bundler packet bundler
     */
    void sendDirtyEntityData(@NotNull PacketBundler bundler);

    /**
     * Sends entity data
     * @param showItem show item
     * @param bundler packet bundler
     */
    void sendEntityData(boolean showItem, @NotNull PacketBundler bundler);

    /**
     * Sets brightness overrides
     * @param block block
     * @param sky sky
     */
    void brightness(int block, int sky);

    /**
     * Sets view range
     * @param range range
     */
    void viewRange(float range);

    /**
     * Sets shadow radius
     * @param radius radius
     */
    void shadowRadius(float radius);

    /**
     * Syncs entity position
     * @param adapter adapter
     * @param bundler packet bundler
     */
    void syncPosition(@NotNull EntityAdapter adapter, @NotNull PacketBundler bundler);

    /**
     * Toggles glow.
     * @param glow glow
     */
    void glow(boolean glow);

    /**
     * Sets glow color.
     * @param glowColor hex glow color
     */
    void glowColor(int glowColor);

    /**
     * Sets display billboard
     * @param billboard billboard
     */
    void billboard(@NotNull Display.Billboard billboard);
}
