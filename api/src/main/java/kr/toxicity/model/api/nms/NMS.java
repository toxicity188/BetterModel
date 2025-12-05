/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
 * A vanilla code handler of Minecraft (NMS)
 */
public interface NMS {

    /**
     * Creates model display
     * @param location start location
     * @return model display
     */
    default @NotNull ModelDisplay create(@NotNull Location location) {
        return create(location, 0, d -> {});
    }

    /**
     * Creates model display
     * @param location start location
     * @param initialConsumer initial consumer
     * @return model display
     */
    default @NotNull ModelDisplay create(@NotNull Location location, @NotNull Consumer<ModelDisplay> initialConsumer) {
        return create(location, 0, initialConsumer);
    }

    /**
     * Creates model display
     * @param location start location
     * @param yOffset y offset
     * @param initialConsumer initial consumer
     * @return model display
     */
    @NotNull ModelDisplay create(@NotNull Location location, double yOffset, @NotNull Consumer<ModelDisplay> initialConsumer);

    /**
     * Creates model nametag
     * @return nametag
     */
    @NotNull ModelNametag createNametag(@NotNull RenderedBone bone);

    /**
     * Creates model nametag
     * @param consumer consumer
     * @return nametag
     */
    default @NotNull ModelNametag createNametag(@NotNull RenderedBone bone, @NotNull Consumer<ModelNametag> consumer) {
        var created = createNametag(bone);
        consumer.accept(created);
        return created;
    }

    /**
     * Injects netty channel handler to player
     * @param player player
     * @return channel handler
     */
    @NotNull PlayerChannelHandler inject(@NotNull Player player);

    /**
     * Creates packet bundler
     * @param initialCapacity initial capacity
     * @return packet bundler
     */
    @NotNull PacketBundler createBundler(int initialCapacity);

    /**
     * Creates lazy packet bundler
     * @return packet bundler
     */
    @NotNull PacketBundler createLazyBundler();

    /**
     * Creates parallel packet bundler
     * @param threshold size threshold
     * @return parallel packet bundler
     */
    @NotNull PacketBundler createParallelBundler(int threshold);

    /**
     * Applies tint to some item
     * @param itemStack item
     * @param rgb hex rgb value
     * @return new item
     */
    @NotNull ItemStack tint(@NotNull ItemStack itemStack, int rgb);

    /**
     * Adds a mount packet for this tracker to bundler
     * @param registry registry
     * @param bundler bundler
     */
    void mount(@NotNull EntityTrackerRegistry registry, @NotNull PacketBundler bundler);

    /**
     * Sends a hide packet for some entity to some player
     * @param channel channel handler
     * @param registry registry
     */
    void hide(@NotNull PlayerChannelHandler channel, @NotNull EntityTrackerRegistry registry);

    /**
     * Sends a hide packet for some entity to some player
     * @param channel channel handler
     * @param registry registry
     * @param condition condition
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
     * Creates delegator hit-box of target entity
     * @param entity target entity
     * @param bone following bone
     * @param namedBoundingBox bounding box
     * @param controller mount controller
     * @param listener hitbox listener
     * @return hit-box
     */
    @Nullable HitBox createHitBox(@NotNull BaseEntity entity, @NotNull RenderedBone bone, @NotNull NamedBoundingBox namedBoundingBox, @NotNull MountController controller, @NotNull HitBoxListener listener);

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
    @NotNull BaseBukkitEntity adapt(@NotNull Entity entity);

    /**
     * Gets adapted player value getter of target player for Folia
     * @param player player
     * @return adapter
     */
    @NotNull BaseBukkitPlayer adapt(@NotNull Player player);

    /**
     * Gets model skin from player
     * @param player player
     * @return model skin
     */
    @NotNull ModelProfile profile(@NotNull Player player);

    /**
     * Creates player head from game skin
     * @param profile skin
     * @return player head item
     */
    @NotNull ItemStack createPlayerHead(@NotNull ModelProfile profile);

    /**
     * Creates skin item
     * @return item
     */
    default @NotNull TransformedItemStack createSkinItem(@NotNull String model, @NotNull List<Boolean> flags, @NotNull List<String> strings, @NotNull List<Integer> colors) {
        return TransformedItemStack.empty();
    }

    /**
     * Gets this server is serviced as online-mode by proxy or native
     * @return is online-mode
     */
    boolean isProxyOnlineMode();
}
