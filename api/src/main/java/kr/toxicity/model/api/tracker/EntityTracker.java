package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.AnimationModifier;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.entity.RenderedEntity;
import kr.toxicity.model.api.entity.TrackerMovement;
import kr.toxicity.model.api.nms.EntityAdapter;
import kr.toxicity.model.api.nms.HitBoxListener;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Getter
public class EntityTracker extends Tracker {
    private static final Map<UUID, EntityTracker> TRACKER_MAP = new ConcurrentHashMap<>();

    private final @NotNull Entity entity;
    private final @NotNull EntityAdapter adapter;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean forRemoval = new AtomicBoolean();
    private final AtomicBoolean autoSpawn = new AtomicBoolean(true);

    public @NotNull UUID world() {
        return entity.getWorld().getUID();
    }

    public static @Nullable EntityTracker tracker(@NotNull Entity entity) {
        var t = tracker(entity.getUniqueId());
        if (t == null) {
            var tag = entity.getPersistentDataContainer().get(TRACKING_ID, PersistentDataType.STRING);
            if (tag == null) return null;
            var render = BetterModel.inst().modelManager().renderer(tag);
            if (render != null) return render.create(entity);
        }
        return t;
    }
    public static @Nullable EntityTracker tracker(@NotNull UUID uuid) {
        return TRACKER_MAP.get(uuid);
    }

    public static void reload() {
        for (EntityTracker value : new ArrayList<>(TRACKER_MAP.values())) {
            Entity target = value.entity;
            var loc = target.getLocation();
            BetterModel.inst().scheduler().task(loc, () -> {
                String name;
                try (value) {
                    if (value.forRemoval()) return;
                    name = value.name();
                } catch (Exception e) {
                    return;
                }
                var renderer = BetterModel.inst().modelManager().renderer(name);
                if (renderer != null) renderer.create(target).spawnNearby(loc);
            });
        }
    }

    public static @NotNull List<EntityTracker> trackers(@NotNull Predicate<EntityTracker> predicate) {
        return TRACKER_MAP.values().stream().filter(predicate).toList();
    }

    public EntityTracker(@NotNull Entity entity, @NotNull RenderInstance instance) {
        super(instance);
        this.entity = entity;
        adapter = entity instanceof LivingEntity livingEntity ? BetterModel.inst().nms().adapt(livingEntity) : EntityAdapter.EMPTY;
        instance.defaultPosition(new Vector3f(0, -adapter.passengerPosition().y, 0));
        instance.addAnimationMovementModifier(
                r -> r.getName().startsWith("h_"),
                a -> {
                    if (a.rotation() != null && !isRunningSingleAnimation()) {
                        a.rotation().add(-adapter.pitch(), Math.clamp(
                                -adapter.yaw() + adapter.bodyYaw(),
                                -45,
                                45
                        ), 0);
                    }
                });
        instance.animateLoop("walk", new AnimationModifier(adapter::onWalk, 0, 0));
        Supplier<TrackerMovement> supplier = () -> new TrackerMovement(
                new Vector3f(0, 0, 0F),
                new Vector3f((float) adapter.scale()),
                new Vector3f(0, -adapter.bodyYaw(), 0)
        );
        setMovement(supplier);
        entity.getPersistentDataContainer().set(TRACKING_ID, PersistentDataType.STRING, instance.getParent().getParent().name());
        TRACKER_MAP.put(entity.getUniqueId(), this);
        BetterModel.inst().scheduler().task(entity.getLocation(), () -> {
            if (!closed.get() && !forRemoval()) createHitBox();
        });
        instance.setup(supplier.get());
    }

    @Override
    public double height() {
        return super.height() + adapter.passengerPosition().y;
    }

    public void createHitBox() {
        createHitBox(e -> e.getName().equals("hitbox") || e.getName().startsWith("b_"));
    }

    public void createHitBox(@NotNull Predicate<RenderedEntity> predicate) {
        createHitBox(predicate, HitBoxListener.EMPTY);
    }

    public void createHitBox(@NotNull Predicate<RenderedEntity> predicate, @NotNull HitBoxListener listener) {
        instance.createHitBox(entity, predicate, listener);
    }

    public void forRemoval(boolean removal) {
        forRemoval.set(removal);
    }

    public boolean forRemoval() {
        return forRemoval.get();
    }

    @Override
    public void close() throws Exception {
        closed.set(true);
        for (PlayerChannelHandler playerChannelHandler : instance.allPlayer()) {
            playerChannelHandler.endTrack(this);
        }
        super.close();
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

    public boolean autoSpawn() {
        return autoSpawn.get();
    }
    public void autoSpawn(boolean spawn) {
        autoSpawn.set(spawn);
    }

    public void spawnNearby(@NotNull Location location) {
        var filter = instance.spawnFilter();
        for (Entity e : location.getWorld().getNearbyEntities(location, EntityUtil.RENDER_DISTANCE , EntityUtil.RENDER_DISTANCE , EntityUtil.RENDER_DISTANCE)) {
            if (e instanceof Player player && filter.test(player)) spawn(player);
        }
    }

    public void spawn(@NotNull Player player) {
        BetterModel.inst().nms().hide(player, entity);
        var bundler = BetterModel.inst().nms().createBundler();
        spawn(player, bundler);
        BetterModel.inst().nms().mount(this, bundler);
        bundler.send(player);
        var handler = BetterModel.inst()
                .playerManager()
                .player(player.getUniqueId());
        if (handler != null) handler.startTrack(this);
    }

    @Override
    public void remove(@NotNull Player player) {
        super.remove(player);
        var handler = BetterModel.inst()
                .playerManager()
                .player(player.getUniqueId());
        if (handler != null) handler.endTrack(this);
    }

    public void refresh() {
        instance.createHitBox(entity, r -> r.getHitBox() != null, null);
        var bundler = BetterModel.inst().nms().createBundler();
        BetterModel.inst().nms().mount(this, bundler);
        if (!bundler.isEmpty()) for (Player player : viewedPlayer()) {
            bundler.send(player);
        }
    }
}
