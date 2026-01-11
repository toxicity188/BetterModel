/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.platform.PlatformLocation;
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

    public static double renderDistance() {
        return BetterModel.platform().adapter().serverViewDistance() << 3;
    }

    public static float entityModelViewRadius() {
        return (float) BetterModel.platform().adapter().serverViewDistance() / 4;
    }

    /**
     * Checks this player can see that entity
     * @param player player's location
     * @param target target's location
     * @return whether target is in user's screen
     */
    public static boolean canSee(@NotNull PlatformLocation player, @NotNull PlatformLocation target) {
        var manager = BetterModel.config();
        if (!manager.sightTrace()) return true;
        else if (player.world() != target.world()) return false;

        var d = distance(player, target);
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
    public static boolean isCustomNameVisible(@NotNull PlatformLocation player, @NotNull PlatformLocation target) {
        if (player.world() != target.world()) return false;
        if (distance(player, target) > 5) return false;
        return isInPoint(player, target);
    }

    private static double distance(@NotNull PlatformLocation a, @NotNull PlatformLocation b) {
        return sqrt(pow(a.x() - b.x(), 2) + pow(a.z() - b.z(), 2));
    }

    /**
     * Checks this target is in player's point
     * @param player player's location
     * @param target target's location
     * @return whether target is player's point
     */
    public static boolean isInPoint(@NotNull PlatformLocation player, @NotNull PlatformLocation target) {
        return isInDegree(player, target, IN_POINT_THRESHOLD, IN_POINT_THRESHOLD);
    }

    private static boolean isInDegree(@NotNull PlatformLocation player, @NotNull PlatformLocation target, double ty, double tz) {
        var playerYaw = toRadians(player.yaw());
        var playerPitch = -toRadians(player.pitch());

        var dz = target.z() - player.z();
        var dy = target.y() - player.y();
        var dx = target.x() - player.x();

        var ry = abs(atan2(dy, sqrt(MathUtil.fma(dz, dz, dx * dx))) - playerPitch);
        var rz = abs(atan2(-dx, dz) - playerYaw);
        return (ry <= ty || ry >= PI * 2 - ty) && (rz <= tz || rz >= PI * 2 - tz);
    }
}
