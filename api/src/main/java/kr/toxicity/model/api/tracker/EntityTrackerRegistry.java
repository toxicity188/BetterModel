package kr.toxicity.model.api.tracker;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.config.DebugConfig;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.util.CollectionUtil;
import kr.toxicity.model.api.util.LogUtil;
import kr.toxicity.model.api.util.lock.DuplexLock;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public final class EntityTrackerRegistry {

    private static final Object2ObjectMap<UUID, EntityTrackerRegistry> UUID_REGISTRY_MAP = new Object2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<EntityTrackerRegistry> ID_REGISTRY_MAP = new Int2ObjectOpenHashMap<>();
    private static final DuplexLock REGISTRY_LOCK = new DuplexLock();
    /**
     * Tracker's namespace.
     */
    public static final NamespacedKey TRACKING_ID = Objects.requireNonNull(NamespacedKey.fromString("bettermodel_tracker"));

    private final AtomicBoolean closed = new AtomicBoolean();
    private final Entity entity;
    private final int id;
    private final UUID uuid;
    private final EntityAdapter adapter;
    private final ConcurrentNavigableMap<String, EntityTracker> trackerMap = new ConcurrentSkipListMap<>();
    private final Collection<EntityTracker> trackers = Collections.unmodifiableCollection(trackerMap.values());
    private final Map<UUID, PlayerChannelCache> viewedPlayerMap = new ConcurrentHashMap<>();
    final Map<UUID, MountedHitBox> mountedHitBoxCache = new ConcurrentHashMap<>();
    private final Map<UUID, MountedHitBox> mountedHitBox = Collections.unmodifiableMap(mountedHitBoxCache);

    public static @Nullable EntityTrackerRegistry registry(@NotNull UUID uuid) {
        return REGISTRY_LOCK.accessToReadLock(() -> UUID_REGISTRY_MAP.get(uuid));
    }

    public static @Nullable EntityTrackerRegistry registry(int id) {
        return REGISTRY_LOCK.accessToReadLock(() -> ID_REGISTRY_MAP.get(id));
    }

    public static void registries(@NotNull Consumer<EntityTrackerRegistry> consumer) {
        for (EntityTrackerRegistry registry : registries()) {
            consumer.accept(registry);
        }
    }
    public static @NotNull @Unmodifiable List<EntityTrackerRegistry> registries() {
        return REGISTRY_LOCK.accessToReadLock(() -> ImmutableList.copyOf(UUID_REGISTRY_MAP.values()));
    }

    public static @Nullable EntityTrackerRegistry registry(@NotNull Entity entity) {
        return hasModelData(entity) ? getOrCreate(entity) : null;
    }

    @ApiStatus.Internal
    public static @NotNull EntityTrackerRegistry getOrCreate(@NotNull Entity entity) {
        var uuid = entity.getUniqueId();
        var get = registry(uuid);
        if (get != null) return get;
        EntityTrackerRegistry registry;
        synchronized (uuid) {
            var get2 = registry(uuid);
            if (get2 != null) return get2;
            registry = new EntityTrackerRegistry(entity);
            REGISTRY_LOCK.accessToWriteLock(() -> {
                UUID_REGISTRY_MAP.put(registry.uuid, registry);
                ID_REGISTRY_MAP.put(registry.id, registry);
                return null;
            });
        }
        registry.load();
        registry.refreshPlayer();
        return registry;
    }

    private static @NotNull Collection<JsonElement> deserialize(@Nullable String raw) {
        if (raw == null) return Collections.emptyList();
        var json = JsonParser.parseString(raw);
        return json.isJsonArray() ? json.getAsJsonArray().asList() : Collections.singletonList(json);
    }

    public static boolean hasModelData(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().has(TRACKING_ID);
    }

    private EntityTrackerRegistry(@NotNull Entity entity) {
        this.entity = entity;
        this.adapter = BetterModel.plugin().nms().adapt(entity);
        this.uuid = entity.getUniqueId();
        this.id = adapter.id();
    }

    public @NotNull Entity entity() {
        return entity;
    }

    public @NotNull UUID uuid() {
        return uuid;
    }

    public int id() {
        return id;
    }

    public @NotNull EntityAdapter adapter() {
        return adapter;
    }

    public @NotNull @Unmodifiable Collection<EntityTracker> trackers() {
        return trackers;
    }

    public @Nullable EntityTracker tracker(@Nullable String key) {
        return key == null ? first() : trackerMap.get(key);
    }

    public @Nullable EntityTracker first() {
        var entry = trackerMap.firstEntry();
        return entry != null ? entry.getValue() : null;
    }

    @ApiStatus.Internal
    public @NotNull EntityTracker create(@NotNull String key, @NotNull Function<EntityTrackerRegistry, EntityTracker> supplier) {
        var created = supplier.apply(this);
        if (putTracker(key, created)) {
            refreshSpawn();
            save();
        }
        return created;
    }

    public @NotNull EntityTracker getOrCreate(@NotNull String key, @NotNull Function<EntityTrackerRegistry, EntityTracker> supplier) {
        var get = trackerMap.get(key);
        return get != null ? get : create(key, supplier);
    }

    private boolean putTracker(@NotNull String key, @NotNull EntityTracker created) {
        if (created.isClosed()) return false;
        created.handleCloseEvent((t, r) -> {
            if (isClosed()) return;
            if (trackerMap.compute(key, (k, v) -> v == created ? null : v) == null) {
                LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> uuid + "'s tracker " + key + " has been removed. (" + trackerMap.size() + ")");
            }
            if (trackerMap.isEmpty() && close(r)) {
                LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> uuid + "'s tracker registry has been removed. (" + UUID_REGISTRY_MAP.size() + ")");
            } else refreshRemove();
        });
        var previous = trackerMap.put(key, created);
        if (previous != null) previous.close();
        return true;
    }

    private void refreshSpawn() {
        viewedPlayer().forEach(value -> spawn(value.player(), true));
    }

    private void refreshRemove() {
        for (PlayerChannelCache value : viewedPlayerMap.values()) {
            value.hide();
        }
    }

    private void refreshPlayer() {
        var stream = adapter.trackedPlayer().stream();
        if (entity instanceof Player player) stream = Stream.concat(Stream.of(player), stream);
        stream.map(p -> BetterModel.player(p.getUniqueId()).orElse(null))
                .filter(Objects::nonNull)
                .forEach(this::registerPlayer);
    }

    public boolean remove(@NotNull String key) {
        try (var removed = trackerMap.remove(key)) {
            save();
            return removed != null;
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    public boolean close() {
        return close(Tracker.CloseReason.REMOVE);
    }

    private boolean close(@NotNull Tracker.CloseReason reason) {
        if (!closed.compareAndSet(false, true)) return false;
        viewedPlayer().forEach(value -> value.sendEntityData(this));
        viewedPlayerMap.clear();
        for (EntityTracker value : trackers()) {
            value.close(reason);
        }
        if (!reason.shouldBeSave()) runSync(() -> entity.getPersistentDataContainer().remove(TRACKING_ID));
        REGISTRY_LOCK.accessToWriteLock(() -> {
            UUID_REGISTRY_MAP.remove(uuid);
            ID_REGISTRY_MAP.remove(id);
            if (entity instanceof Player player) player.updateInventory();
            return null;
        });
        return true;
    }

    public void reload() {
        closed.set(true);
        var data = new ArrayList<TrackerData>(trackerMap.size());
        for (EntityTracker value : trackers()) {
            data.add(value.asTrackerData());
            value.close();
        }
        trackerMap.clear();
        closed.set(false);
        load(data.stream());
    }

    public void refresh() {
        if (adapter.dead()) return;
        for (EntityTracker value : trackers()) {
            value.refresh();
        }
        refreshPlayer();
        refreshSpawn();
    }

    public void despawn() {
        for (EntityTracker value : trackers()) {
            if (!value.forRemoval()) value.despawn();
        }
        viewedPlayerMap.clear();
    }

    public void load(@NotNull Stream<TrackerData> stream) {
        stream.forEach(parsed -> BetterModel.model(parsed.id()).ifPresent(model -> model.create(entity, parsed.modifier(), parsed::applyAs)));
        save();
    }

    public void load() {
        load(deserialize(entity.getPersistentDataContainer().get(TRACKING_ID, PersistentDataType.STRING))
                .stream()
                .map(TrackerData::deserialize));
    }

    public void save() {
        var data = serialize().toString();
        runSync(() -> entity.getPersistentDataContainer().set(TRACKING_ID, PersistentDataType.STRING, data));
    }

    private void runSync(@NotNull Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else BetterModel.plugin().scheduler().task(entity.getLocation(), runnable);
    }

    public @NotNull Stream<ModelDisplay> displays() {
        return trackers()
                .stream()
                .flatMap(Tracker::displays);
    }

    public @NotNull JsonArray serialize() {
        return CollectionUtil.mapToJson(trackers(), value -> value.asTrackerData().serialize());
    }

    /**
     * Checks this tracker is spawned by some player
     * @param player player
     * @return is spawned
     */
    public boolean isSpawned(@NotNull Player player) {
        return isSpawned(player.getUniqueId());
    }
    /**
     * Checks this tracker is spawned by some player
     * @param uuid player's uuid
     * @return is spawned
     */
    public boolean isSpawned(@NotNull UUID uuid) {
        return viewedPlayerMap.containsKey(uuid) && trackers()
                .stream()
                .anyMatch(t -> t.pipeline.isSpawned(uuid));
    }

    /**
     * Spawns this tracker to some player
     * @param player target player
     * @return success
     */
    public boolean spawn(@NotNull Player player) {
        return spawn(player, false);
    }
    private boolean spawn(@NotNull Player player, boolean shouldNotSpawned) {
        var handler = BetterModel.plugin()
                .playerManager()
                .player(player.getUniqueId());
        if (handler == null) return false;
        var cache = registerPlayer(handler);
        if (trackerMap.isEmpty()) return false;
        var bundler = BetterModel.plugin().nms().createBundler(10);
        for (EntityTracker value : trackers()) {
            if (shouldNotSpawned && value.pipeline.isSpawned(player.getUniqueId())) continue;
            if (value.canBeSpawnedAt(player)) value.spawn(player, bundler);
        }
        if (bundler.isEmpty()) return false;
        BetterModel.plugin().nms().mount(this, bundler);
        cache.spawn(bundler);
        return true;
    }

    private @NotNull PlayerChannelCache registerPlayer(@NotNull PlayerChannelHandler handler) {
        return viewedPlayerMap.computeIfAbsent(handler.uuid(), u -> new PlayerChannelCache(handler));
    }

    public @NotNull Stream<PlayerChannelHandler> viewedPlayer() {
        return viewedPlayerMap.values().stream().map(c -> c.channelHandler);
    }

    public boolean remove(@NotNull Player player) {
        var cache = viewedPlayerMap.remove(player.getUniqueId());
        if (cache == null) return false;
        var handler = cache.channelHandler;
        handler.sendEntityData(this);
        for (EntityTracker value : trackers()) {
            if (!value.forRemoval() && value.pipeline.isSpawned(player.getUniqueId())) value.remove(handler.player());
        }
        return true;
    }

    public @NotNull EntityHideOption hideOption(@NotNull UUID uuid) {
        var cache = viewedPlayerMap.get(uuid);
        return cache != null ? cache.hideOption : EntityHideOption.FALSE;
    }

    /**
     * Gets currently mounted hitbox
     * @return mounted hitbox
     */
    @NotNull
    @Unmodifiable
    public Map<UUID, MountedHitBox> mountedHitBox() {
        return mountedHitBox;
    }
    
    /**
     * Checks this tracker has passenger
     * @return has passenger
     */
    public boolean hasPassenger() {
        return !mountedHitBox().isEmpty();
    }

    /**
     * Checks this tracker has controlling passenger
     * @return has controlling passenger
     */
    public boolean hasControllingPassenger() {
        return mountedHitBox()
                .values()
                .stream()
                .map(MountedHitBox::hitBox)
                .anyMatch(HitBox::hasBeenControlled);
    }


    /**
     * Hitbox with mount info
     * @param entity entity
     * @param hitBox hitbox
     */
    public record MountedHitBox(@NotNull Entity entity, @NotNull HitBox hitBox) {
        /**
         * Dismount this entity from hitbox.
         */
        public void dismount() {
            hitBox.dismount(entity);
        }
        /**
         * Dismount all entities from hitbox.
         */
        public void dismountAll() {
            hitBox.dismountAll();
        }
    }

    @RequiredArgsConstructor
    private class PlayerChannelCache {
        private final PlayerChannelHandler channelHandler;
        private volatile EntityHideOption hideOption = EntityHideOption.DEFAULT;

        private void hide() {
            reapplyHideOption();
            BetterModel.plugin().nms().hide(channelHandler, EntityTrackerRegistry.this);
        }

        private void spawn(@NotNull PacketBundler bundler) {
            reapplyHideOption();
            bundler.send(channelHandler.player(), () -> BetterModel.plugin().nms().hide(channelHandler, EntityTrackerRegistry.this, () -> viewedPlayerMap.containsKey(channelHandler.uuid())));
        }

        private synchronized void reapplyHideOption() {
            hideOption = EntityHideOption.composite(trackers()
                    .stream()
                    .filter(t -> t.pipeline.isSpawned(channelHandler.uuid()))
                    .map(EntityTracker::hideOption));
        }
    }
}
