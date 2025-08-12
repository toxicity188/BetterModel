package kr.toxicity.model.api.util;

import kr.toxicity.model.api.BetterModel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import static java.lang.Math.*;

/**
 * Entity
 */
@ApiStatus.Internal
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
    public static final double RENDER_DISTANCE = Bukkit.getViewDistance() << 3;

    /**
     * Entity model view radius.
     */
    public static final float ENTITY_MODEL_VIEW_RADIUS = (float) Bukkit.getViewDistance() / 4;

    /**
     * Y-axis threshold of user screen.
     */
    private static final double Y_RENDER_THRESHOLD = toRadians(45);
    /**
     * In point threshold of user screen.
     */
    private static final double IN_POINT_THRESHOLD = toRadians(10);
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
        var manager = BetterModel.config();
        if (!manager.sightTrace()) return true;
        else if (player.getWorld() != target.getWorld()) return false;

        var d = player.distance(target);
        if (d > manager.maxSight()) return false;
        else if (d <= manager.minSight()) return true;

        var t = PI - abs(atan(d)) * 2;
        var ty = t + Y_RENDER_THRESHOLD;
        var tz = t + X_RENDER_THRESHOLD;
        return isInDegree(player, target, ty, tz);
    }

    /**
     * Checks this target's custom name is visible at player
     * @param player player's location
     * @param target target's location
     * @return whether target's custom name is visible
     */
    public static boolean isCustomNameVisible(@NotNull Location player, @NotNull Location target) {
        if (player.getWorld() != target.getWorld()) return false;
        if (player.distance(target) > 5) return false;
        return isInPoint(player, target);
    }
    /**
     * Checks this target is in player's point
     * @param player player's location
     * @param target target's location
     * @return whether target is player's point
     */
    public static boolean isInPoint(@NotNull Location player, @NotNull Location target) {
        return isInDegree(player, target, IN_POINT_THRESHOLD, IN_POINT_THRESHOLD);
    }

    private static boolean isInDegree(@NotNull Location player, @NotNull Location target, double ty, double tz) {
        var playerYaw = toRadians(player.getYaw());
        var playerPitch = -toRadians(player.getPitch());

        var dz = target.getZ() - player.getZ();
        var dy = target.getY() - player.getY();
        var dx = target.getX() - player.getX();

        var ry = abs(atan2(dy, sqrt(MathUtil.fma(dz, dz, dx * dx))) - playerPitch);
        var rz = abs(atan2(-dx, dz) - playerYaw);
        return (ry <= ty || ry >= PI * 2 - ty) && (rz <= tz || rz >= PI * 2 - tz);
    }
}
