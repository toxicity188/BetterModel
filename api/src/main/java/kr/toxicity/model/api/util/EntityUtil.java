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
 * Utility class for entity-related calculations, primarily visibility checks.
 * <p>
 * This class provides methods to determine if an entity is within a player's field of view
 * or render distance, optimizing client-side rendering performance.
 * </p>
 *
 * @since 2.0.0
 */
@ApiStatus.Internal
public final class EntityUtil {

    /**
     * Private initializer to prevent instantiation.
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

    /**
     * Calculates the render distance in blocks based on the server's view distance.
     *
     * @return the render distance
     * @since 2.0.0
     */
    public static double renderDistance() {
        return BetterModel.platform().adapter().serverViewDistance() << 3;
    }

    /**
     * Calculates the entity model view radius based on the server's view distance.
     *
     * @return the view radius
     * @since 2.0.0
     */
    public static float entityModelViewRadius() {
        return (float) BetterModel.platform().adapter().serverViewDistance() / 4;
    }

    /**
     * Checks if a player can see a target entity based on sight tracing configuration.
     *
     * @param player the player's location
     * @param target the target entity's location
     * @return true if the target is visible, false otherwise
     * @since 2.0.0
     */
    public static boolean canSee(@NotNull PlatformLocation player, @NotNull PlatformLocation target) {
        var manager = BetterModel.config();
        if (!manager.sightTrace()) return true;
        else if (!player.world().equals(target.world())) return false;

        var d = distance(player, target);
        if (d > manager.maxSight()) return false;
        else if (d <= manager.minSight()) return true;

        var t = PI - abs(atan(d)) * 2;
        var ty = t + Y_RENDER_THRESHOLD;
        var tz = t + X_RENDER_THRESHOLD;
        return isInDegree(player, target, ty, tz);
    }

    /**
     * Checks if a target entity's custom name is visible to a player.
     *
     * @param player the player's location
     * @param target the target entity's location
     * @return true if the custom name is visible, false otherwise
     * @since 2.0.0
     */
    public static boolean isCustomNameVisible(@NotNull PlatformLocation player, @NotNull PlatformLocation target) {
        if (!player.world().equals(target.world())) return false;
        if (distance(player, target) > 5) return false;
        return isInPoint(player, target);
    }

    private static double distance(@NotNull PlatformLocation a, @NotNull PlatformLocation b) {
        return sqrt(pow(a.x() - b.x(), 2) + pow(a.z() - b.z(), 2));
    }

    /**
     * Checks if a target entity is directly in the player's crosshair (point of view).
     *
     * @param player the player's location
     * @param target the target entity's location
     * @return true if the target is in the player's point of view
     * @since 2.0.0
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
