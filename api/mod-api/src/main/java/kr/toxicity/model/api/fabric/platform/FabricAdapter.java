/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric.platform;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.fabric.BetterModelFabric;
import kr.toxicity.model.api.platform.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FabricAdapter implements PlatformAdapter {

    public static @NotNull PlatformEntity adapt(@NotNull Entity entity) {
        return new FabricEntity(entity);
    }

    public static @NotNull PlatformLivingEntity adapt(@NotNull LivingEntity livingEntity) {
        return new FabricLivingEntity(livingEntity);
    }

    public static @NotNull PlatformPlayer adapt(@NotNull ServerPlayer player) {
        return new FabricPlayer(player);
    }

    public static @NotNull PlatformOfflinePlayer adapt(@NotNull UUID uuid) {
        return new FabricOfflinePlayer(uuid, null);
    }

    public static @NotNull PlatformOfflinePlayer adapt(@NotNull GameProfile profile) {
        return new FabricOfflinePlayer(profile.id(), profile.name());
    }

    public static @NotNull PlatformItemStack adapt(@NotNull ItemStack itemStack) {
        return new FabricItemStack(itemStack);
    }

    public static @NotNull PlatformWorld adapt(@NotNull Level world) {
        return new FabricWorld(world);
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
        return isTickThread();
    }

    @Override
    public @Nullable PlatformPlayer player(@NotNull UUID uuid) {
        var player = server().getPlayerList().getPlayer(uuid);
        return player == null ? null : new FabricPlayer(player);
    }

    @Override
    public @NotNull PlatformOfflinePlayer offlinePlayer(@NotNull UUID uuid) {
        var profile = server().services().profileResolver().fetchById(uuid).orElse(null);
        return profile == null ? FabricAdapter.adapt(uuid) : FabricAdapter.adapt(profile);
    }

    @Override
    public @NotNull PlatformItemStack air() {
        return new FabricItemStack(ItemStack.EMPTY);
    }

    @Override
    public @NotNull PlatformLocation zero() {
        return new FabricLocation(null, 0, 0, 0, 0, 0);
    }

    private @NotNull MinecraftServer server() {
        return ((BetterModelFabric) BetterModel.platform()).server();
    }
}
