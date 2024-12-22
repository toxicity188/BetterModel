package kr.toxicity.model.api.util;

import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox;
import kr.toxicity.model.api.nms.NMSVersion;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

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
