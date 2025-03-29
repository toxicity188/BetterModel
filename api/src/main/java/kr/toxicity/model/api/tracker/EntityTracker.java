package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.AnimationModifier;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.nms.EntityAdapter;
import kr.toxicity.model.api.nms.HitBoxListener;
import kr.toxicity.model.api.util.BonePredicate;
import kr.toxicity.model.api.util.EntityUtil;
import kr.toxicity.model.api.util.FunctionUtil;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Getter
public class EntityTracker extends Tracker {
    private static final Map<UUID, EntityTracker> TRACKER_MAP = new ConcurrentHashMap<>();

    private final @NotNull Entity entity;
    @Getter
    private final @NotNull EntityAdapter adapter;
    private final AtomicBoolean forRemoval = new AtomicBoolean();
    private final AtomicBoolean autoSpawn = new AtomicBoolean(true);

    private final AtomicInteger damageTintValue = new AtomicInteger(0xFF7979);
    private final AtomicLong damageTint = new AtomicLong(-1);

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
                if (renderer != null) renderer.create(target, value.modifier()).spawnNearby(loc);
            });
        }
    }

    @NotNull
    @Override
    public ModelRotation rotation() {
        return adapter.dead() ? instance.getRotation() : new ModelRotation(0, entity instanceof LivingEntity ? adapter.bodyYaw() : entity.getYaw());
    }

    public static @NotNull List<EntityTracker> trackers(@NotNull Predicate<EntityTracker> predicate) {
        return TRACKER_MAP.values().stream().filter(predicate).toList();
    }

    public EntityTracker(@NotNull Entity entity, @NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        super(instance, modifier);
        this.entity = entity;
        adapter = (entity instanceof LivingEntity livingEntity ? BetterModel.inst().nms().adapt(livingEntity) : EntityAdapter.EMPTY)
                .multiply(modifier.scale());
        instance.defaultPosition(new Vector3f(0, -adapter.passengerPosition().y, 0));
        instance.addAnimationMovementModifier(
                BonePredicate.of(r -> r.getName().startsWith("h_")),
                a -> {
                    if (a.rotation() != null && !isRunningSingleAnimation()) {
                        a.rotation().add(-adapter.pitch(), Math.clamp(
                                -adapter.yaw() + adapter.bodyYaw(),
                                -45,
                                45
                        ), 0);
                    }
                });
        instance.addAnimationMovementModifier(
                BonePredicate.of(true, r -> r.getName().startsWith("hi_")),
                a -> {
                    if (a.rotation() != null && !isRunningSingleAnimation()) {
                        a.rotation().add(-adapter.pitch(), Math.clamp(
                                -adapter.yaw() + adapter.bodyYaw(),
                                -45,
                                45
                        ), 0);
                    }
                });

        var damageTickProvider = FunctionUtil.throttleTick(adapter::damageTick);
        var walkSupplier = FunctionUtil.throttleTick(() -> adapter.onWalk() || damageTickProvider.get() > 0.25 || instance.renderers().stream().anyMatch(e -> {
            var hitBox = e.getHitBox();
            return hitBox != null && hitBox.onWalk();
        }));
        var walkSpeedSupplier = FunctionUtil.throttleTick(modifier.damageEffect() ? () -> adapter.walkSpeed() + 4F * (float) Math.sqrt(damageTickProvider.get()) : () -> 1F);
        instance.animateLoop("walk", new AnimationModifier(walkSupplier, 4, 0, walkSpeedSupplier));
        instance.animateLoop("idle_fly", new AnimationModifier(adapter::fly, 4, 0, 1F));
        instance.animateLoop("walk_fly", new AnimationModifier(() -> adapter.fly() && walkSupplier.get(), 4, 0, walkSpeedSupplier));

        Supplier<TrackerMovement> supplier = () -> new TrackerMovement(
                new Vector3f(0, 0, 0F),
                new Vector3f((float) adapter.scale()),
                new Vector3f(0, 0, 0)
        );
        setMovement(supplier);
        TRACKER_MAP.put(entity.getUniqueId(), this);
        BetterModel.inst().scheduler().task(entity.getLocation(), () -> {
            entity.getPersistentDataContainer().set(TRACKING_ID, PersistentDataType.STRING, instance.getParent().getParent().name());
            if (!isClosed() && !forRemoval()) createHitBox();
        });
        tick(t -> t.displays().forEach(d -> d.sync(adapter)));
        tick(t -> {
            var reader = t.instance.getScriptProcessor().getCurrentReader();
            if (reader == null) return;
            var script = reader.script();
            if (script == null) return;
            BetterModel.inst().scheduler().task(entity.getLocation(), () -> script.accept(entity));
        });
        frame(t -> {
            if (damageTint.getAndDecrement() == 0) tint(0xFFFFFF);
        });
        update();
    }

    @Override
    public double height() {
        return super.height() + adapter.passengerPosition().y;
    }

    public void createHitBox() {
        createHitBox(e -> e.getName().contains("hitbox") || e.getName().startsWith("b_") || e.getGroup().getMountController().canMount());
    }

    public void createHitBox(@NotNull Predicate<RenderedBone> predicate) {
        createHitBox(predicate, HitBoxListener.EMPTY);
    }

    public void createHitBox(@NotNull Predicate<RenderedBone> predicate, @NotNull HitBoxListener listener) {
        instance.createHitBox(adapter, predicate, listener);
    }

    public int damageTintValue() {
        return damageTintValue.get();
    }

    public void damageTintValue(int tint) {
        damageTintValue.set(tint);
    }

    public void damageTint() {
        if (!modifier().damageEffect()) return;
        var get = damageTint.get();
        if (get <= 0 && damageTint.compareAndSet(get, 50)) tint(damageTintValue.get());
    }

    public void forRemoval(boolean removal) {
        forRemoval.set(removal);
    }

    public boolean forRemoval() {
        return forRemoval.get();
    }

    @Override
    public void close() throws Exception {
        instance.allPlayer().forEach(p -> p.endTrack(this));
        super.close();
        TRACKER_MAP.remove(entity.getUniqueId());
        BetterModel.inst().scheduler().task(entity.getLocation(), () -> entity.getPersistentDataContainer().remove(TRACKING_ID));
    }

    @Override
    public void despawn() {
        instance.allPlayer().forEach(p -> p.endTrack(this));
        super.despawn();
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

    public void spawnNearby() {
        spawnNearby(location());
    }

    public @NotNull Entity source() {
        return entity;
    }

    public void spawnNearby(@NotNull Location location) {
        var filter = instance.spawnFilter();
        for (Entity e : location.getWorld().getNearbyEntities(location, EntityUtil.RENDER_DISTANCE , EntityUtil.RENDER_DISTANCE , EntityUtil.RENDER_DISTANCE)) {
            if (e instanceof Player player && filter.test(player)) spawn(player);
        }
    }

    public boolean canBeSpawnedAt(@NotNull Player player) {
        return autoSpawn() && instance.spawnFilter().test(player);
    }

    public void spawn(@NotNull Player player) {
        var bundler = BetterModel.inst().nms().createBundler();
        spawn(player, bundler);
        BetterModel.inst().nms().mount(this, bundler);
        bundler.send(player);
        var handler = BetterModel.inst()
                .playerManager()
                .player(player.getUniqueId());
        if (handler != null) handler.startTrack(this);
        BetterModel.inst().nms().hide(player, entity);
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
        BetterModel.inst().scheduler().task(location(), () -> instance.createHitBox(adapter, r -> r.getHitBox() != null, null));
        var bundler = BetterModel.inst().nms().createBundler();
        BetterModel.inst().nms().mount(this, bundler);
        if (!bundler.isEmpty()) viewedPlayer().forEach(bundler::send);
    }
}
