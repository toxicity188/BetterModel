package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.renderer.AnimationModifier;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.entity.TrackerMovement;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.util.EntityUtil;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@Getter
public final class EntityTracker extends Tracker {
    private static final Map<UUID, EntityTracker> TRACKER_MAP = new ConcurrentHashMap<>();

    private final @NotNull Entity entity;
    @Getter
    private HitBox hitBox;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean forRemoval = new AtomicBoolean();

    public static @Nullable EntityTracker tracker(@NotNull Entity entity) {
        var t = tracker(entity.getUniqueId());
        if (t == null) {
            var tag = entity.getPersistentDataContainer().get(TRACKING_ID, PersistentDataType.STRING);
            if (tag == null) return null;
            var render = ModelRenderer.inst().modelManager().renderer(tag);
            if (render != null) return render.create(entity);
        }
        return t;
    }
    public static @Nullable EntityTracker tracker(@NotNull UUID uuid) {
        return TRACKER_MAP.get(uuid);
    }
    public static @NotNull List<EntityTracker> trackers(@NotNull Predicate<EntityTracker> predicate) {
        return TRACKER_MAP.values().stream().filter(predicate).toList();
    }

    public EntityTracker(@NotNull Entity entity, @NotNull RenderInstance instance) {
        super(() -> new TrackerMovement(
                new Vector3f(0, -ModelRenderer.inst().nms().passengerPosition(entity).y, 0F),
                new Vector3f((float) ModelRenderer.inst().nms().scale(entity)),
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
            addForceUpdateConstraint(t -> EntityUtil.onWalk(livingEntity));
            instance.animateLoop("walk", new AnimationModifier(() -> EntityUtil.onWalk(livingEntity), 0, 0));
        }
        refreshHitBox();
        entity.getPersistentDataContainer().set(TRACKING_ID, PersistentDataType.STRING, instance.getParent().getParent().name());
        TRACKER_MAP.put(entity.getUniqueId(), this);
    }

    public void refreshHitBox() {
        if (hitBox != null) hitBox.remove();
        var box = instance.hitBox();
        if (box != null) {
            hitBox = ModelRenderer.inst().nms().createHitBox(entity, box.box());
        }
    }

    public void forRemoval(boolean removal) {
        forRemoval.set(removal);
    }

    public boolean forRemoval() {
        return forRemoval.get();
    }

    @Override
    public void close() throws Exception {
        if (closed.get()) return;
        closed.set(true);
        super.close();
        if (hitBox != null) hitBox.remove();
        entity.getPersistentDataContainer().remove(TRACKING_ID);
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

    public void spawn(@NotNull Player player) {
        var bundler = ModelRenderer.inst().nms().createBundler();
        spawn(player, bundler);
        ModelRenderer.inst().nms().mount(this, bundler);
        var handler = ModelRenderer.inst()
                .playerManager()
                .player(player.getUniqueId());
        if (handler != null) handler.startTrack(this);
        bundler.send(player);
    }

    public @NotNull List<ModelDisplay> renderers() {
        return instance.renderers();
    }

    @Override
    public void remove(@NotNull Player player) {
        super.remove(player);
        var handler = ModelRenderer.inst()
                .playerManager()
                .player(player.getUniqueId());
        if (handler != null) handler.endTrack(this);
    }
}
