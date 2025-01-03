package kr.toxicity.model.api.util;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

import static java.lang.Math.*;

/**
 * Entity
 */
public final class EntityUtil {

    /**
     * No initializer
     */
    private EntityUtil() {
        throw new RuntimeException();
    }

    /**
     * Default render distance.
     */
    public static final double RENDER_DISTANCE = Bukkit.getSimulationDistance() << 3;

    /**
     * Y-axis threshold of user screen.
     */
    private static final double Y_RENDER_THRESHOLD = toRadians(22.5);
    /**
     * X-axis threshold of user screen.
     */
    private static final double X_RENDER_THRESHOLD = Y_RENDER_THRESHOLD * 1.78;

    /**
     * Checks this player can see that entity
     * @param player player's location
     * @param target target's location
     * @return whether target is in user's screen
     */
    public static boolean canSee(@NotNull Location player, @NotNull Location target) {
        var manager = BetterModel.inst().configManager();
        if (!manager.sightTrace()) return true;
        else if (player.getWorld() != target.getWorld()) return false;

        var d = player.distance(target);
        if (d > manager.maxSight()) return false;
        else if (d <= manager.minSight()) return true;

        var playerYaw = toRadians(player.getYaw());
        var playerPitch = toRadians(-player.getPitch());

        var dx = target.getZ() - player.getZ();
        var dy = target.getY() - player.getY();
        var dz = -(target.getX() - player.getX());

        var r = cos(playerYaw) * dx - sin(playerYaw) * dz;

        var sinP = sin(playerPitch);
        var cosP = cos(playerPitch);

        var ry = abs(atan2(dy, abs(r)) - playerPitch);
        var rz = abs(atan2(dz, dx) - playerYaw);
        var ty = PI - abs(atan(sinP * r + cosP * dy)) * 2 + Y_RENDER_THRESHOLD;
        var tz = PI - abs(atan(cosP * r + sinP * dy)) * 2 + X_RENDER_THRESHOLD;
        return (ry <= ty || ry >= PI * 2 - ty) && (rz <= tz || rz >= PI * 2 - tz);
    }

    /**
     * Gets max hit-box size.
     * @param target hit-box list
     * @return max hit-box
     */
    public static @Nullable ModelBoundingBox max(@NotNull List<ModelBoundingBox> target) {
        return target.stream()
                .max(Comparator.comparingDouble(b -> Math.sqrt(Math.pow(b.maxX() - b.minX(), 2) + Math.pow(b.maxY() - b.minY(), 2) + Math.pow(b.maxZ() - b.minZ(), 2))))
                .orElse(null);
    }
}
