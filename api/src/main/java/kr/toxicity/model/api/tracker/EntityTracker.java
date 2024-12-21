package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.entity.TrackerMovement;
import kr.toxicity.model.api.nms.ModelDisplay;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public final class EntityTracker extends Tracker {
    private static final Map<UUID, EntityTracker> TRACKER_MAP = new ConcurrentHashMap<>();

    private final @NotNull Entity entity;
    private final AtomicBoolean closed = new AtomicBoolean();

    public static @Nullable EntityTracker tracker(@NotNull Entity entity) {
        return tracker(entity.getUniqueId());
    }
    public static @Nullable EntityTracker tracker(@NotNull UUID uuid) {
        return TRACKER_MAP.get(uuid);
    }

    public EntityTracker(@NotNull Entity entity, @NotNull RenderInstance instance) {
        super(() -> new TrackerMovement(
                new Vector3f(0, entity instanceof LivingEntity livingEntity ? (float) -livingEntity.getEyeHeight() - 0.1F : 0, 0F),
                new Vector3f(1),
                new Vector3f(0, entity instanceof LivingEntity livingEntity ? -livingEntity.getBodyYaw() : -entity.getYaw(), 0)
        ), instance);
        this.entity = entity;
        if (entity instanceof LivingEntity livingEntity) {
            instance.addAnimationMovementModifier(
                    r -> r.getName().startsWith("h_"),
                    a -> {
                        if (a.rotation() != null && !isRunningSingleAnimation()) {
                            a.rotation().add(-entity.getPitch(), Math.clamp(
                                    -livingEntity.getYaw() + livingEntity.getBodyYaw(),
                                    -45,
                                    45
                            ), 0);
                        }
                    });
            instance.animateLoop("walk", () -> {
                double speed = Optional.ofNullable(livingEntity.getAttribute(Attribute.MOVEMENT_SPEED))
                        .map(AttributeInstance::getValue)
                        .orElse(0.2);
                return entity.isOnGround() && entity.getVelocity().length() / speed > 0.4;
            });
            instance.animateLoop("run", () -> {
                double speed = Optional.ofNullable(livingEntity.getAttribute(Attribute.MOVEMENT_SPEED))
                        .map(AttributeInstance::getValue)
                        .orElse(0.2);
                return entity.isOnGround() && entity.getVelocity().length() / speed > 0.45;
            });
        }
        TRACKER_MAP.put(entity.getUniqueId(), this);
    }

    @Override
    public void close() throws Exception {
        if (closed.get()) return;
        closed.set(true);
        super.close();
        if (entity.isValid()) entity.remove();
        TRACKER_MAP.remove(entity.getUniqueId());
    }

    @Override
    public @NotNull Location location() {
        return entity.getLocation();
    }

    @Override
    public @NotNull UUID uuid() {
        return entity.getUniqueId();
    }

    @Override
    public void spawn(@NotNull Player player) {
        super.spawn(player);
        ModelRenderer.inst().playerManager().player(player).startTrack(this);
    }

    public @NotNull List<ModelDisplay> renderers() {
        return instance.renderers();
    }

    @Override
    public void remove(@NotNull Player player) {
        super.remove(player);
        ModelRenderer.inst().playerManager().player(player).endTrack(this);
    }

}
