package kr.toxicity.model.api.util;

import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox;
import kr.toxicity.model.api.nms.NMSVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static java.lang.Math.*;

public final class EntityUtil {
    private EntityUtil() {
        throw new RuntimeException();
    }

    public static double movement(@NotNull LivingEntity entity) {
        var attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(ModelRenderer.inst().nms().version().compareTo(NMSVersion.V1_21_R2) >= 0 ? "movement_speed" : "generic.movement_speed"));
        var speed = entity.getAttribute(Objects.requireNonNull(attribute));
        return speed != null ? speed.getValue() : 0.2;
    }

    public static final double RENDER_DISTANCE = Bukkit.getSimulationDistance() << 4;

    public static boolean onWalk(@NotNull LivingEntity entity) {
        double speed = movement(entity);
        //System.out.println(DecimalFormat.getInstance().format(speed) + " : " + DecimalFormat.getInstance().format(entity.getVelocity().length() / speed));
        return entity.isOnGround() && entity.getVelocity().length() / speed > 0.4;
    }

    public static final double MAX_RADIUS = 45;
    public static final double MIN_RADIUS = 3;

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

        var ry = abs(atan2(dy, abs(dx)) - playerPitch);
        var rz = abs(atan2(dz, dx) - playerYaw);
        var ty = PI - abs(atan(cos(playerPitch) * dx - sin(playerPitch) * dy));
        var tz = PI - abs(atan(cos(playerYaw) * dx - sin(playerYaw) * dz));
        return (ry <= ty || ry >= PI * 2 - ty) && (rz <= tz || rz >= PI * 2 - tz);
    }

    public static @NotNull NamedBoundingBox box(@NotNull String name, @NotNull List<VectorPair> elements) {
        var dx = Float.MAX_VALUE;
        var dy = Float.MAX_VALUE;
        var dz = Float.MAX_VALUE;
        var tx = Float.MIN_VALUE;
        var ty = Float.MIN_VALUE;
        var tz = Float.MIN_VALUE;
        for (VectorPair element : elements) {
            var d = element.min();
            var t = element.max();

            if (d.x < dx) dx = d.x;
            if (d.y < dy) dy = d.y;
            if (d.z < dz) dz = d.z;
            if (t.x > tx) tx = t.x;
            if (t.y > ty) ty = t.y;
            if (t.x > tx) tz = t.z;
        }
        return new NamedBoundingBox(name, new BoundingBox(
                dx, dy, dz,
                tx, ty, tz
        ));
    }
}
