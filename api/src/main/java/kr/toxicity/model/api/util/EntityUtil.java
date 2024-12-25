package kr.toxicity.model.api.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

import static java.lang.Math.*;

public final class EntityUtil {
    private EntityUtil() {
        throw new RuntimeException();
    }

    public static final double RENDER_DISTANCE = Bukkit.getSimulationDistance() << 4;

    public static final double MAX_RADIUS = 45;
    public static final double MIN_RADIUS = 5;

    public static boolean canSee(@NotNull Location player, @NotNull Location target) {
        if (player.getWorld() != target.getWorld()) return false;

        var d = player.distance(target);
        if (d > MAX_RADIUS) return false;
        if (d <= MIN_RADIUS) return true;

        var playerYaw = toRadians(player.getYaw());
        var playerPitch = toRadians(-player.getPitch()) * 2;

        var dx = target.getZ() - player.getZ();
        var dy = target.getY() - player.getY();
        var dz = -(target.getX() - player.getX());

        var r = cos(playerYaw) * dx - sin(playerYaw) * dz;

        var ry = abs(atan2(dy, abs(dx)) - playerPitch);
        var rz = abs(atan2(dz, dx) - playerYaw);
        var ty = PI - abs(atan(cos(playerPitch) * r - sin(playerPitch) * dy));
        var tz = PI - abs(atan(r));
        return (ry <= ty || ry >= PI * 2 - ty) && (rz <= tz || rz >= PI * 2 - tz);
    }

    public static @Nullable BoundingBox max(@NotNull List<BoundingBox> target) {
        return target.stream()
                .max(Comparator.comparingDouble(b -> Math.sqrt(Math.pow(b.getMaxX() - b.getMinX(), 2) + Math.pow(b.getMaxY() - b.getMinY(), 2) + Math.pow(b.getMaxZ() - b.getMinZ(), 2))))
                .orElse(null);
    }
}
