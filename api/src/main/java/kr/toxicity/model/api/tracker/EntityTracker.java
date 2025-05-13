package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.BoneTags;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.event.CreateEntityTrackerEvent;
import kr.toxicity.model.api.nms.EntityAdapter;
import kr.toxicity.model.api.nms.HitBoxListener;
import kr.toxicity.model.api.util.BonePredicate;
import kr.toxicity.model.api.util.EntityUtil;
import kr.toxicity.model.api.util.EventUtil;
import kr.toxicity.model.api.util.FunctionUtil;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Entity tracker
 */
public class EntityTracker extends Tracker {
    private static final Map<UUID, EntityTracker> TRACKER_MAP = new ConcurrentHashMap<>();
    private static final Collection<EntityTracker> TRACKERS_VIEW = Collections.unmodifiableCollection(TRACKER_MAP.values());

    private final @NotNull Entity entity;
    @Getter
    private final @NotNull EntityAdapter adapter;
    private final AtomicBoolean forRemoval = new AtomicBoolean();
    private final AtomicBoolean autoSpawn = new AtomicBoolean(true);

    private final AtomicInteger damageTintValue = new AtomicInteger(0xFF7979);
    private final AtomicLong damageTint = new AtomicLong(-1);

    /**
     * Gets world uuid in tracker
     * @return world
     */
    public @NotNull UUID world() {
        return entity.getWorld().getUID();
    }

    /**
     * Get or creates tracker
     * @param entity source
     * @return tracker or null
     */
    public static @Nullable EntityTracker tracker(@NotNull Entity entity) {
        var t = tracker(entity.getUniqueId());
        if (t == null) {
            var tag = entity.getPersistentDataContainer().get(TRACKING_ID, PersistentDataType.STRING);
            if (tag == null) return null;
            return BetterModel.model(tag)
                    .map(renderer -> renderer.create(entity))
                    .orElse(null);
        }
        return t;
    }

    public static @NotNull @Unmodifiable Collection<EntityTracker> trackers() {
        return TRACKERS_VIEW;
    }

    /**
     * Gets tracker
     * @param uuid entity's uuid
     * @return tracker or null
     */
    public static @Nullable EntityTracker tracker(@NotNull UUID uuid) {
        return TRACKER_MAP.get(uuid);
    }

    /**
     * Reloads entity tracker
     */
    @ApiStatus.Internal
    public static void reload() {
        for (EntityTracker value : new ArrayList<>(TRACKER_MAP.values())) {
            Entity target = value.entity;
            BetterModel.inst().scheduler().task(target, () -> {
                String name;
                try (value) {
                    if (value.forRemoval()) return;
                    name = value.name();
                } catch (Exception e) {
                    return;
                }
                BetterModel.model(name).ifPresent(renderer -> renderer.create(target, value.modifier()).spawnNearby());
            });
        }
    }

    /**
     * Creates entity tracker
     * @param source source entity
     * @param instance render instance
     * @param modifier modifier
     */
    @ApiStatus.Internal
    public EntityTracker(@NotNull RenderSource.Based source, @NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        super(source, instance, modifier);
        this.entity = source.entity();
        adapter = BetterModel.inst().nms().adapt(entity);
        var scale = FunctionUtil.throttleTick(() -> modifier.scale().get() * (float) adapter.scale());
        //Shadow
        if (modifier.shadow()) {
            var shadow = BetterModel.inst().nms().create(entity.getLocation());
            var baseScale = (float) instance.bones()
                    .stream()
                    .filter(b -> b.getGroup().getParent().visibility())
                    .map(b -> b.getGroup().getHitBox())
                    .filter(Objects::nonNull)
                    .mapToDouble(b -> b.box().lengthZX() / 2)
                    .max()
                    .orElse(0D);
            tick(((t, b) -> {
                shadow.shadowRadius(scale.get() * baseScale);
                shadow.sync(adapter);
                shadow.sendEntityData(b);
                shadow.syncPosition(adapter, b);
            }));
            instance.spawnPacketHandler(shadow::spawn);
            instance.despawnPacketHandler(shadow::remove);
        }

        //Animation
        instance.defaultPosition(FunctionUtil.throttleTick(() -> adapter.passengerPosition().mul(-1)));
        instance.scale(scale);
        instance.addAnimationMovementModifier(
                BonePredicate.of(r -> r.getName().tagged(BoneTags.HEAD)),
                a -> {
                    if (a.rotation() != null) {
                        a.rotation().add(-adapter.pitch(), Math.clamp(
                                -adapter.yaw() + adapter.bodyYaw(),
                                -45,
                                45
                        ), 0);
                    }
                });

        var damageTickProvider = FunctionUtil.throttleTick(adapter::damageTick);
        var walkSupplier = FunctionUtil.throttleTick(() -> adapter.onWalk() || damageTickProvider.get() > 0.25 || instance.bones().stream().anyMatch(e -> {
            var hitBox = e.getHitBox();
            return hitBox != null && hitBox.onWalk();
        }));
        var walkSpeedSupplier = FunctionUtil.throttleTick(modifier.damageEffect() ? () -> adapter.walkSpeed() + 4F * (float) Math.sqrt(damageTickProvider.get()) : () -> 1F);
        instance.animate("walk", new AnimationModifier(walkSupplier, 6, 0, AnimationIterator.Type.LOOP, walkSpeedSupplier));
        instance.animate("idle_fly", new AnimationModifier(adapter::fly, 6, 0, AnimationIterator.Type.LOOP, 1F));
        instance.animate("walk_fly", new AnimationModifier(() -> adapter.fly() && walkSupplier.get(), 6, 0, AnimationIterator.Type.LOOP, walkSpeedSupplier));
        instance.animate("spawn", AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
        TRACKER_MAP.put(entity.getUniqueId(), this);
        BetterModel.inst().scheduler().task(entity, () -> {
            if (isClosed()) return;
            entity.getPersistentDataContainer().set(TRACKING_ID, PersistentDataType.STRING, instance.getParent().getParent().name());
            createHitBox();
        });
        tick((t, b) -> updateBaseEntity0());
        tick((t, b) -> {
            var reader = t.instance.getScriptProcessor().getCurrentReader();
            if (reader == null) return;
            var script = reader.script();
            if (script == null) return;
            BetterModel.inst().scheduler().task(entity, () -> script.accept(entity));
        });
        tick(2, (t, b) -> {
            if (adapter.dead() && !forRemoval()) close();
        });
        frame((t, b) -> {
            if (damageTint.getAndDecrement() == 0) tint(0xFFFFFF);
        });
        rotation(() -> adapter.dead() ? instance.getRotation() : new ModelRotation(0, entity instanceof LivingEntity ? adapter.bodyYaw() : entity.getYaw()));
        update();
        EventUtil.call(new CreateEntityTrackerEvent(this));
    }

    private void createHitBox() {
        createHitBox(e ->
                e.getName().name().equals("hitbox")
                        || e.getName().tagged(BoneTags.HITBOX)
                        || e.getGroup().getMountController().canMount()
        );
    }

    private void createHitBox(@NotNull Predicate<RenderedBone> predicate) {
        createHitBox(predicate, HitBoxListener.EMPTY);
    }

    public void updateBaseEntity() {
        BetterModel.inst().scheduler().taskLater(1, entity, () -> {
            updateBaseEntity0();
            forceUpdate(true);
        });
    }

    /**
     * Updates base entity's data to parent entity
     */
    private void updateBaseEntity0() {
        displays().forEach(d -> d.sync(adapter));
    }

    /**
     * Creates hit-box
     * @param predicate predicate
     * @param listener listener
     */
    public void createHitBox(@NotNull Predicate<RenderedBone> predicate, @NotNull HitBoxListener listener) {
        instance.createHitBox(adapter, predicate, listener);
    }

    /**
     * Gets damage tint value
     * @return value
     */
    public int damageTintValue() {
        return damageTintValue.get();
    }

    /**
     * Sets damage tint value
     * @param tint hex color
     */
    public void damageTintValue(int tint) {
        damageTintValue.set(tint);
    }

    /**
     * Applies damage tint
     */
    public void damageTint() {
        if (!modifier().damageEffect()) return;
        var get = damageTint.get();
        if (get <= 0 && damageTint.compareAndSet(get, 50)) tint(damageTintValue.get());
    }

    /**
     * Marks future will remove this tracker
     * @param removal removal
     */
    @ApiStatus.Internal
    public void forRemoval(boolean removal) {
        forRemoval.set(removal);
    }

    /**
     * Checks this tracker is for removal
     * @return for removal
     */
    @ApiStatus.Internal
    public boolean forRemoval() {
        return forRemoval.get();
    }

    @Override
    public void close() {
        instance.allPlayer().forEach(p -> p.endTrack(this));
        super.close();
        TRACKER_MAP.remove(entity.getUniqueId());
        entity.getPersistentDataContainer().remove(TRACKING_ID);
        if (entity instanceof Player player) player.updateInventory();
    }

    @Override
    public void despawn() {
        if (adapter.dead()) {
            close();
            return;
        }
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

    /**
     * Checks this tracker supports auto spawn
     * @return auto spawn
     */
    public boolean autoSpawn() {
        return autoSpawn.get();
    }

    /**
     * Sets this tracker is auto-spawned by some player's client
     * @param spawn auto spawn
     */
    public void autoSpawn(boolean spawn) {
        autoSpawn.set(spawn);
    }


    /**
     * Gets source entity
     * @return source
     */
    public @NotNull Entity sourceEntity() {
        return entity;
    }

    /**
     * Spawns this tracker ranged by view distance
     */
    public void spawnNearby() {
        spawnNearby(location());
    }

    /**
     * Spawns this tracker ranged by view distance
     * @param location center location
     */
    public void spawnNearby(@NotNull Location location) {
        var filter = instance.spawnFilter();
        for (Entity e : location.getWorld().getNearbyEntities(location, EntityUtil.RENDER_DISTANCE , EntityUtil.RENDER_DISTANCE , EntityUtil.RENDER_DISTANCE)) {
            if (e instanceof Player player && filter.test(player)) spawn(player);
        }
    }

    /**
     * Checks this tracker can be spawned at this player's client
     * @param player target player
     * @return can be spawned at
     */
    public boolean canBeSpawnedAt(@NotNull Player player) {
        return autoSpawn() && instance.spawnFilter().test(player);
    }

    /**
     * Sets move duration of this model.
     * @param duration duration
     */
    public void moveDuration(int duration) {
        instance.moveDuration(duration);
        forceUpdate(true);
    }

    /**
     * Spawns this tracker to some player
     * @param player target player
     * @return success
     */
    public boolean spawn(@NotNull Player player) {
        var bundler = instance.createBundler();
        if (!spawn(player, bundler)) return false;
        BetterModel.inst().nms().mount(this, bundler);
        bundler.send(player, () -> BetterModel.inst().nms().hide(player, entity));
        var handler = BetterModel.inst()
                .playerManager()
                .player(player.getUniqueId());
        if (handler != null) handler.startTrack(this);
        return true;
    }

    @Override
    public boolean remove(@NotNull Player player) {
        if (!super.remove(player)) return false;
        var handler = BetterModel.inst()
                .playerManager()
                .player(player.getUniqueId());
        if (handler != null) handler.endTrack(this);
        return true;
    }

    /**
     * Refresh this tracker
     */
    @ApiStatus.Internal
    public void refresh() {
        BetterModel.inst().scheduler().task(entity, () -> instance.createHitBox(adapter, r -> r.getHitBox() != null, null));
    }
}
