/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.platform;

import kr.toxicity.model.api.bukkit.BetterModelBukkit;
import kr.toxicity.model.api.platform.*;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Provides an adapter for converting Bukkit objects to BetterModel platform objects.
 * <p>
 * This class implements {@link PlatformAdapter} and offers static utility methods for adapting
 * entities, players, items, locations, and worlds.
 * </p>
 *
 * @since 2.0.0
 */
public final class BukkitAdapter implements PlatformAdapter {

    /**
     * Adapts a Bukkit entity to a {@link PlatformEntity}.
     *
     * @param entity the Bukkit entity
     * @return the platform entity
     * @since 2.0.0
     */
    public static @NotNull PlatformEntity adapt(@NotNull Entity entity) {
        return new BukkitEntity(entity);
    }

    /**
     * Adapts a Bukkit living entity to a {@link PlatformLivingEntity}.
     *
     * @param livingEntity the Bukkit living entity
     * @return the platform living entity
     * @since 2.0.0
     */
    public static @NotNull PlatformLivingEntity adapt(@NotNull LivingEntity livingEntity) {
        return new BukkitLivingEntity(livingEntity);
    }

    /**
     * Adapts a Bukkit offline player to a {@link PlatformOfflinePlayer}.
     *
     * @param player the Bukkit offline player
     * @return the platform offline player
     * @since 2.0.0
     */
    public static @NotNull PlatformOfflinePlayer adapt(@NotNull OfflinePlayer player) {
        return new BukkitOfflinePlayer(player);
    }

    /**
     * Adapts a Bukkit player to a {@link PlatformPlayer}.
     *
     * @param player the Bukkit player
     * @return the platform player
     * @since 2.0.0
     */
    public static @NotNull PlatformPlayer adapt(@NotNull Player player) {
        return new BukkitPlayer(player);
    }

    /**
     * Adapts a Bukkit item stack to a {@link PlatformItemStack}.
     *
     * @param itemStack the Bukkit item stack
     * @return the platform item stack
     * @since 2.0.0
     */
    public static @NotNull PlatformItemStack adapt(@NotNull ItemStack itemStack) {
        return new BukkitItemStack(itemStack);
    }

    /**
     * Adapts a Bukkit location to a {@link PlatformLocation}.
     *
     * @param location the Bukkit location
     * @return the platform location
     * @since 2.0.0
     */
    public static @NotNull PlatformLocation adapt(@NotNull Location location) {
        return new BukkitLocation(location);
    }

    /**
     * Adapts a Bukkit world to a {@link PlatformWorld}.
     *
     * @param world the Bukkit world
     * @return the platform world
     * @since 2.0.0
     */
    public static @NotNull PlatformWorld adapt(@NotNull World world) {
        return new BukkitWorld(world);
    }

    @Override
    public @Nullable PlatformPlayer player(@NotNull UUID uuid) {
        var bukkit = Bukkit.getPlayer(uuid);
        return bukkit != null ? adapt(bukkit) : null;
    }

    @Override
    public @NotNull PlatformOfflinePlayer offlinePlayer(@NotNull UUID uuid) {
        return adapt(Bukkit.getOfflinePlayer(uuid));
    }

    @Override
    public int serverViewDistance() {
        return Bukkit.getViewDistance();
    }

    @Override
    public boolean isTickThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isRegionSafe() {
        return !BetterModelBukkit.IS_FOLIA || isTickThread();
    }

    @Override
    public @NotNull PlatformItemStack air() {
        return adapt(new ItemStack(Material.AIR));
    }

    @Override
    public @NotNull PlatformLocation zero() {
        return adapt(new Location(null, 0, 0, 0));
    }
}
