/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox;
import kr.toxicity.model.api.entity.BaseBukkitEntity;
import kr.toxicity.model.api.entity.BaseBukkitPlayer;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.entity.BasePlayer;
import kr.toxicity.model.api.mount.MountController;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import kr.toxicity.model.api.util.TransformedItemStack;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Handles direct interactions with Minecraft's internal server code (NMS).
 * <p>
 * This interface provides methods for creating displays, managing packets, handling hitboxes,
 * and adapting entities for different server environments (e.g., Folia).
 * </p>
 *
 * @since 1.15.2
 */
public interface NMS {

    /**
     * Creates a model display at the specified location.
     *
     * @param location the starting location
     * @return the created model display
     * @since 1.15.2
     */
    default @NotNull ModelDisplay create(@NotNull Location location) {
        return create(location, 0, d -> {});
    }

    /**
     * Creates a model display at the specified location with an initial configuration.
     *
     * @param location the starting location
     * @param initialConsumer a consumer to configure the display upon creation
     * @return the created model display
     * @since 1.15.2
     */
    default @NotNull ModelDisplay create(@NotNull Location location, @NotNull Consumer<ModelDisplay> initialConsumer) {
        return create(location, 0, initialConsumer);
    }

    /**
     * Creates a model display at the specified location with a Y-offset and initial configuration.
     *
     * @param location the starting location
     * @param yOffset the vertical offset
     * @param initialConsumer a consumer to configure the display upon creation
     * @return the created model display
     * @since 1.15.2
     */
    @NotNull ModelDisplay create(@NotNull Location location, double yOffset, @NotNull Consumer<ModelDisplay> initialConsumer);

    /**
     * Creates a nametag for a rendered bone.
     *
     * @param bone the bone to attach the nametag to
     * @return the created nametag
     * @since 1.15.2
     */
    @NotNull ModelNametag createNametag(@NotNull RenderedBone bone);

    /**
     * Creates a nametag for a rendered bone with configuration.
     *
     * @param bone the bone to attach the nametag to
     * @param consumer a consumer to configure the nametag
     * @return the created nametag
     * @since 1.15.2
     */
    default @NotNull ModelNametag createNametag(@NotNull RenderedBone bone, @NotNull Consumer<ModelNametag> consumer) {
        var created = createNametag(bone);
        consumer.accept(created);
        return created;
    }

    /**
     * Injects a Netty channel handler into a player's connection.
     *
     * @param player the player to inject
     * @return the created channel handler
     * @since 1.15.2
     */
    @NotNull PlayerChannelHandler inject(@NotNull Player player);

    /**
     * Creates a packet bundler with an initial capacity.
     *
     * @param initialCapacity the initial capacity
     * @return the packet bundler
     * @since 1.15.2
     */
    @NotNull PacketBundler createBundler(int initialCapacity);

    /**
     * Creates a lazy packet bundler.
     *
     * @return the packet bundler
     * @since 1.15.2
     */
    @NotNull PacketBundler createLazyBundler();

    /**
     * Creates a parallel packet bundler with a size threshold.
     *
     * @param threshold the size threshold for parallel processing
     * @return the packet bundler
     * @since 1.15.2
     */
    @NotNull PacketBundler createParallelBundler(int threshold);

    /**
     * Applies a tint color to an item stack.
     *
     * @param itemStack the item to tint
     * @param rgb the RGB color value
     * @return the tinted item stack
     * @since 1.15.2
     */
    @NotNull ItemStack tint(@NotNull ItemStack itemStack, int rgb);

    /**
     * Adds a mount packet for an entity tracker to a bundler.
     *
     * @param registry the entity tracker registry
     * @param bundler the packet bundler
     * @since 1.15.2
     */
    void mount(@NotNull EntityTrackerRegistry registry, @NotNull PacketBundler bundler);

    /**
     * Sends a hide packet for an entity to a player via their channel handler.
     *
     * @param channel the player's channel handler
     * @param registry the entity tracker registry
     * @since 1.15.2
     */
    void hide(@NotNull PlayerChannelHandler channel, @NotNull EntityTrackerRegistry registry);

    /**
     * Sends a hide packet for an entity to a player if a condition is met.
     * <p>
     * For players, the hide operation is delayed based on configuration.
     * </p>
     *
     * @param channel the player's channel handler
     * @param registry the entity tracker registry
     * @param condition the condition to check
     * @since 1.15.2
     */
    default void hide(@NotNull PlayerChannelHandler channel, @NotNull EntityTrackerRegistry registry, @NotNull BooleanSupplier condition) {
        if (registry.entity() instanceof BasePlayer) {
            var plugin = BetterModel.plugin();
            plugin.scheduler().asyncTaskLater(plugin.config().playerHideDelay(), () -> {
                if (condition.getAsBoolean()) hide(channel, registry);
            });
        } else hide(channel, registry);
    }

    /**
     * Creates a delegate hitbox for a target entity.
     *
     * @param entity the target entity
     * @param bone the bone associated with the hitbox
     * @param namedBoundingBox the bounding box definition
     * @param controller the mount controller
     * @param listener the hitbox listener
     * @return the created hitbox, or null if creation failed
     * @since 1.15.2
     */
    @Nullable HitBox createHitBox(@NotNull BaseEntity entity, @NotNull RenderedBone bone, @NotNull NamedBoundingBox namedBoundingBox, @NotNull MountController controller, @NotNull HitBoxListener listener);

    /**
     * Returns the NMS version of the server.
     *
     * @return the version
     * @since 1.15.2
     */
    @NotNull NMSVersion version();

    /**
     * Adapts a Bukkit entity to a {@link BaseBukkitEntity}, handling Folia compatibility.
     *
     * @param entity the Bukkit entity
     * @return the adapted entity
     * @since 1.15.2
     */
    @NotNull BaseBukkitEntity adapt(@NotNull Entity entity);

    /**
     * Adapts a Bukkit player to a {@link BaseBukkitPlayer}, handling Folia compatibility.
     *
     * @param player the Bukkit player
     * @return the adapted player
     * @since 1.15.2
     */
    @NotNull BaseBukkitPlayer adapt(@NotNull Player player);

    /**
     * Retrieves the model profile (skin) for a player.
     *
     * @param player the player
     * @return the model profile
     * @since 1.15.2
     */
    @NotNull ModelProfile profile(@NotNull Player player);

    /**
     * Creates a player head item stack from a model profile.
     *
     * @param profile the model profile
     * @return the player head item
     * @since 1.15.2
     */
    @NotNull ItemStack createPlayerHead(@NotNull ModelProfile profile);

    /**
     * Creates a custom skin item stack.
     *
     * @param model the model name
     * @param flags a list of flags
     * @param strings a list of strings
     * @param colors a list of colors
     * @return the transformed item stack
     * @since 1.15.2
     */
    default @NotNull TransformedItemStack createSkinItem(@NotNull String model, @NotNull List<Boolean> flags, @NotNull List<String> strings, @NotNull List<Integer> colors) {
        return TransformedItemStack.empty();
    }

    /**
     * Checks if the server is in online mode (either natively or via proxy).
     *
     * @return true if online mode, false otherwise
     * @since 1.15.2
     */
    boolean isProxyOnlineMode();
}
