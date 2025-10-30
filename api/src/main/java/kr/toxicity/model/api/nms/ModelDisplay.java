/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
    void syncEntity(@NotNull BaseEntity entity);

    /**
     * Syncs position of source location
     * @param location location
     */
    void syncPosition(@NotNull Location location);

    /**
     * Sets pos-rot movement interpolator duration
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
     * Spawns this display with data
     * @param bundler packet bundler
     */
    default void spawnWithEntityData(@NotNull PacketBundler bundler) {
        var visible = !invisible();
        spawn(visible, bundler);
        sendEntityData(visible, bundler);
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
     * Creates transformer
     * @return transformer
     */
    @NotNull DisplayTransformer createTransformer();

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
     * Sends entity position
     * @param adapter adapter
     * @param bundler packet bundler
     */
    void sendPosition(@NotNull BaseEntity adapter, @NotNull PacketBundler bundler);

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
