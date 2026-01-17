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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FabricAdapter implements PlatformAdapter {
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
        ServerPlayer player = server().getPlayerList().getPlayer(uuid);
        return player == null ?
            null :
            new FabricPlayer(player);
    }

    @Override
    public @NotNull PlatformOfflinePlayer offlinePlayer(@NotNull UUID uuid) {
        GameProfile profile = server().services().profileResolver().fetchById(uuid).orElse(null);
        return profile == null ?
            FabricOfflinePlayer.of(uuid) :
            FabricOfflinePlayer.of(profile);
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
