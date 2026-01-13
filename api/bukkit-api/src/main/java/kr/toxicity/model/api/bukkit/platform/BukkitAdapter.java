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

public final class BukkitAdapter implements PlatformAdapter {

    public static @NotNull PlatformEntity adapt(@NotNull Entity entity) {
        return new BukkitEntity(entity);
    }

    public static @NotNull PlatformLivingEntity adapt(@NotNull LivingEntity livingEntity) {
        return new BukkitLivingEntity(livingEntity);
    }

    public static @NotNull PlatformPlayer adapt(@NotNull Player player) {
        return new BukkitPlayer(player);
    }

    public static @NotNull PlatformOfflinePlayer adapt(@NotNull OfflinePlayer player) {
        return new BukkitOfflinePlayer(player);
    }

    public static @NotNull PlatformItemStack adapt(@NotNull ItemStack itemStack) {
        return new BukkitItemStack(itemStack);
    }

    public static @NotNull PlatformLocation adapt(@NotNull Location location) {
        return new BukkitLocation(location);
    }

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
