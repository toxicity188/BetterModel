/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.tracker.ModelRotation;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an item display entity used for rendering model parts.
 * <p>
 * This interface abstracts the NMS implementation of item displays, allowing for manipulation of
 * position, rotation, scale, and other display properties.
 * </p>
 *
 * @since 1.15.2
 */
public interface ModelDisplay extends Identifiable {

    /**
     * Checks if this display is currently invisible.
     *
     * @return true if invisible, false otherwise
     * @since 1.15.2
     */
    boolean invisible();

    /**
     * Sets the visibility of this display.
     *
     * @param invisible true to make invisible, false to make visible
     * @since 1.15.2
     */
    void invisible(boolean invisible);

    /**
     * Rotates this display to a specific orientation.
     *
     * @param rotation the target rotation
     * @param bundler the packet bundler to use
     * @since 1.15.2
     */
    void rotate(@NotNull ModelRotation rotation, @NotNull PacketBundler bundler);

    /**
     * Synchronizes this display with a source entity's data.
     *
     * @param entity the source entity
     * @since 1.15.2
     */
    void syncEntity(@NotNull BaseEntity entity);

    /**
     * Synchronizes this display's position with a location.
     *
     * @param location the target location
     * @since 1.15.2
     */
    void syncPosition(@NotNull Location location);

    /**
     * Sets the duration for position/rotation interpolation.
     *
     * @param duration the duration in ticks
     * @since 1.15.2
     */
    void moveDuration(int duration);

    /**
     * Sets the item display transform type.
     *
     * @param transform the transform type
     * @since 1.15.2
     */
    void display(@NotNull ItemDisplay.ItemDisplayTransform transform);

    /**
     * Spawns this display using the provided packet bundler.
     *
     * @param bundler the packet bundler
     * @since 1.15.2
     */
    default void spawn(@NotNull PacketBundler bundler) {
        spawn(!invisible(), bundler);
    }

    /**
     * Spawns this display and sends initial entity data.
     *
     * @param bundler the packet bundler
     * @since 1.15.2
     */
    default void spawnWithEntityData(@NotNull PacketBundler bundler) {
        var visible = !invisible();
        spawn(visible, bundler);
        sendEntityData(visible, bundler);
    }

    /**
     * Spawns this display, optionally showing the item.
     *
     * @param showItem true to show the item, false otherwise
     * @param bundler the packet bundler
     * @since 1.15.2
     */
    void spawn(boolean showItem, @NotNull PacketBundler bundler);

    /**
     * Removes this display.
     *
     * @param bundler the packet bundler
     * @since 1.15.2
     */
    void remove(@NotNull PacketBundler bundler);

    /**
     * Teleports this display to a new location.
     *
     * @param location the target location
     * @param bundler the packet bundler
     * @since 1.15.2
     */
    void teleport(@NotNull Location location, @NotNull PacketBundler bundler);

    /**
     * Sets the item stack to be displayed.
     *
     * @param itemStack the item stack
     * @since 1.15.2
     */
    void item(@NotNull ItemStack itemStack);

    /**
     * Creates a transformer for animating this display.
     *
     * @return the display transformer
     * @since 1.15.2
     */
    @NotNull DisplayTransformer createTransformer();

    /**
     * Sends updated entity data if it has changed.
     *
     * @param bundler the packet bundler
     * @since 1.15.2
     */
    void sendDirtyEntityData(@NotNull PacketBundler bundler);

    /**
     * Sends entity data, optionally showing the item.
     *
     * @param showItem true to show the item, false otherwise
     * @param bundler the packet bundler
     * @since 1.15.2
     */
    void sendEntityData(boolean showItem, @NotNull PacketBundler bundler);

    /**
     * Sets the brightness override for the display.
     *
     * @param block the block light level
     * @param sky the skylight level
     * @since 1.15.2
     */
    void brightness(int block, int sky);

    /**
     * Sets the view range of the display.
     *
     * @param range the view range
     * @since 1.15.2
     */
    void viewRange(float range);

    /**
     * Sets the shadow radius of the display.
     *
     * @param radius the shadow radius
     * @since 1.15.2
     */
    void shadowRadius(float radius);

    /**
     * Sends the position of the display relative to an entity adapter.
     *
     * @param adapter the entity adapter
     * @param bundler the packet bundler
     * @since 1.15.2
     */
    void sendPosition(@NotNull BaseEntity adapter, @NotNull PacketBundler bundler);

    /**
     * Toggles the glowing effect.
     *
     * @param glow true to enable glow, false to disable
     * @since 1.15.2
     */
    void glow(boolean glow);

    /**
     * Sets the glow color.
     *
     * @param glowColor the RGB glow color
     * @since 1.15.2
     */
    void glowColor(int glowColor);

    /**
     * Sets the billboard constraint for the display.
     *
     * @param billboard the billboard type
     * @since 1.15.2
     */
    void billboard(@NotNull Display.Billboard billboard);
}
