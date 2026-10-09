/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.mod.platform;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.mod.BetterModelMod;
import kr.toxicity.model.api.platform.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Provides an adapter for converting Mod/NMS objects to BetterModel platform objects.
 * <p>
 * This class implements {@link PlatformAdapter} and offers static utility methods for adapting
 * entities, players, items, and worlds.
 * </p>
 *
 * @since 2.0.0
 */
public final class ModAdapter implements PlatformAdapter {

    /**
     * Adapts an NMS entity to a {@link PlatformEntity}.
     *
     * @param entity the NMS entity
     * @return the platform entity
     * @since 2.0.0
     */
    public static @NotNull PlatformEntity adapt(@NotNull Entity entity) {
        return ModEntity.of(entity);
    }

    /**
     * Adapts an NMS living entity to a {@link PlatformLivingEntity}.
     *
     * @param livingEntity the NMS living entity
     * @return the platform living entity
     * @since 2.0.0
     */
    public static @NotNull PlatformLivingEntity adapt(@NotNull LivingEntity livingEntity) {
        return ModLivingEntity.of(livingEntity);
    }

    /**
     * Adapts an NMS player connection to a {@link PlatformPlayer}.
     *
     * @param connection the NMS player connection
     * @return the platform player
     * @since 2.0.0
     */
    public static @NotNull PlatformPlayer adapt(@NotNull ServerPlayerConnection connection) {
        return ModPlayer.of(connection);
    }

    /**
     * Adapts an NMS server player to a {@link PlatformPlayer}.
     *
     * @param player the NMS server player
     * @return the platform player
     * @since 2.0.0
     */
    public static @NotNull PlatformPlayer adapt(@NotNull ServerPlayer player) {
        return adapt(player.connection);
    }

    /**
     * Adapts a UUID to a {@link PlatformOfflinePlayer}.
     *
     * @param uuid the player UUID
     * @return the platform offline player
     * @since 2.0.0
     */
    public static @NotNull PlatformOfflinePlayer adapt(@NotNull UUID uuid) {
        return ModOfflinePlayer.of(uuid, null);
    }

    /**
     * Adapts a GameProfile to a {@link PlatformOfflinePlayer}.
     *
     * @param profile the game profile
     * @return the platform offline player
     * @since 2.0.0
     */
    public static @NotNull PlatformOfflinePlayer adapt(@NotNull GameProfile profile) {
        return ModOfflinePlayer.of(profile.id(), profile.name());
    }

    /**
     * Adapts an NMS item stack to a {@link PlatformItemStack}.
     *
     * @param itemStack the NMS item stack
     * @return the platform item stack
     * @since 2.0.0
     */
    public static @NotNull PlatformItemStack adapt(@NotNull ItemStack itemStack) {
        return ModItemStack.of(itemStack);
    }

    /**
     * Adapts an NMS level to a {@link PlatformWorld}.
     *
     * @param world the NMS level
     * @return the platform world
     * @since 2.0.0
     */
    public static @NotNull PlatformWorld adapt(@NotNull Level world) {
        return ModWorld.of(world);
    }

    @Override
    public int serverViewDistance() {
        return server().getPlayerList().getViewDistance();
    }

    @Override
    public boolean isTickThread() {
        return server().isSameThread();
    }

    @Override
    public boolean isRegionSafe() {
        return true;
    }

    @Override
    public @Nullable PlatformPlayer player(@NotNull UUID uuid) {
        var player = server().getPlayerList().getPlayer(uuid);
        return player == null ? null : adapt(player);
    }

    @Override
    public @NotNull PlatformOfflinePlayer offlinePlayer(@NotNull UUID uuid) {
        var profile = server().services().profileResolver().fetchById(uuid).orElse(null);
        return profile == null ? adapt(uuid) : adapt(profile);
    }

    @Override
    public @NotNull PlatformItemStack air() {
        return adapt(ItemStack.EMPTY);
    }

    @Override
    public @NotNull PlatformLocation zero() {
        return ModLocation.of(null, 0, 0, 0);
    }

    private @NotNull MinecraftServer server() {
        return BetterModelMod.platform().server();
    }
}
