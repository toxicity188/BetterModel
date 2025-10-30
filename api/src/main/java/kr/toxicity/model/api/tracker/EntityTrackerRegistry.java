/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.config.DebugConfig;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.entity.BasePlayer;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.util.CollectionUtil;
import kr.toxicity.model.api.util.LogUtil;
import kr.toxicity.model.api.util.ThreadUtil;
import kr.toxicity.model.api.util.lock.DuplexLock;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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

/**
 * A registry of each entity's tracker
 */
@ToString(onlyExplicitlyIncluded = true)
public final class EntityTrackerRegistry {

    private static final Object2ReferenceMap<UUID, EntityTrackerRegistry> UUID_REGISTRY_MAP = new Object2ReferenceOpenHashMap<>();
    private static final Int2ReferenceMap<EntityTrackerRegistry> ID_REGISTRY_MAP = new Int2ReferenceOpenHashMap<>();
    private static final DuplexLock REGISTRY_LOCK = new DuplexLock();
    /**
     * Tracker's namespace.
     */
    @NotNull
    public static final NamespacedKey TRACKING_ID = Objects.requireNonNull(NamespacedKey.fromString("bettermodel_tracker"));

    @ToString.Include
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean loaded = new AtomicBoolean();
    @ToString.Include
    private final BaseEntity entity;
    private final int id;
    private final UUID uuid;
    private final ConcurrentNavigableMap<String, EntityTracker> trackerMap = new ConcurrentSkipListMap<>();
    @ToString.Include
    private final Collection<EntityTracker> trackers = Collections.unmodifiableCollection(trackerMap.values());
    private final Map<UUID, PlayerChannelCache> viewedPlayerMap = new ConcurrentHashMap<>();
    final Map<UUID, MountedHitBox> mountedHitBoxCache = new ConcurrentHashMap<>();
    private final Map<UUID, MountedHitBox> mountedHitBox = Collections.unmodifiableMap(mountedHitBoxCache);

    /**
     * Gets registry by uuid
     * @param uuid uuid
     * @return registry or null
     */
    public static @Nullable EntityTrackerRegistry registry(@NotNull UUID uuid) {
        return REGISTRY_LOCK.accessToReadLock(() -> UUID_REGISTRY_MAP.get(uuid));
    }

    /**
     * Gets registry by id
     * @param id id
     * @return registry or null
     */
    public static @Nullable EntityTrackerRegistry registry(int id) {
        return REGISTRY_LOCK.accessToReadLock(() -> ID_REGISTRY_MAP.get(id));
    }

    /**
     * Gets registry by entity
     * @param entity entity
     * @return registry or null
     */
    public static @Nullable EntityTrackerRegistry registry(@NotNull BaseEntity entity) {
        return entity.hasModelData() ? getOrCreate(entity) : null;
    }

    /**
     * Uses all registries
     * @param consumer consumer
     */
    public static void registries(@NotNull Consumer<EntityTrackerRegistry> consumer) {
        for (EntityTrackerRegistry registry : registries()) {
            consumer.accept(registry);
        }
    }

    /**
     * Gets all registries
     * @return all registries
     */
    public static @NotNull @Unmodifiable List<EntityTrackerRegistry> registries() {
        return REGISTRY_LOCK.accessToReadLock(() -> ImmutableList.copyOf(UUID_REGISTRY_MAP.values()));
    }

    /**
     * Gets or creates registry by entity
     * @param entity entity
     * @return registry
     */
    @ApiStatus.Internal
    public static @NotNull EntityTrackerRegistry getOrCreate(@NotNull BaseEntity entity) {
        var uuid = entity.uuid();
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
        registry.initialLoad();
        return registry;
    }

    private static @NotNull Collection<JsonElement> deserialize(@Nullable String raw) {
        if (raw == null) return Collections.emptyList();
        var json = JsonParser.parseString(raw);
        return json.isJsonArray() ? json.getAsJsonArray().asList() : Collections.singletonList(json);
    }

    private EntityTrackerRegistry(@NotNull BaseEntity entity) {
        this.entity = entity;
        this.uuid = entity.uuid();
        this.id = entity.id();
    }

    /**
     * Gets source entity
     * @return entity
     */
    public @NotNull BaseEntity entity() {
        return entity;
    }

    /**
     * Gets entity's uuid
     * @return uuid
     */
    public @NotNull UUID uuid() {
        return uuid;
    }

    /**
     * Gets entity's id
     * @return id
     */
    public int id() {
        return id;
    }

    /**
     * Gets all trackers of this registry
     * @return all trackers
     */
    public @NotNull @Unmodifiable Collection<EntityTracker> trackers() {
        return trackers;
    }

    /**
     * Gets some tracker by its name
     * @param key key
     * @return tracker or null
     */
    public @Nullable EntityTracker tracker(@Nullable String key) {
        return key == null ? first() : trackerMap.get(key);
    }

    /**
     * Gets first tracker of this register
     * @return first tracker
     */
    public @Nullable EntityTracker first() {
        var entry = trackerMap.firstEntry();
        return entry != null ? entry.getValue() : null;
    }

    /**
     * Creates tracker in this registry
     * @param key key
     * @param supplier supplier
     * @return created tracker
     */
    @ApiStatus.Internal
    public @NotNull EntityTracker create(@NotNull String key, @NotNull Function<EntityTrackerRegistry, EntityTracker> supplier) {
        var created = supplier.apply(this);
        if (putTracker(key, created)) {
            refreshSpawn();
            save();
        }
        return created;
    }

    /**
     * Gets or creates tracker in this registry
     * @param key key
     * @param supplier supplier
     * @return created tracker
     */
    @ApiStatus.Internal
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
            if (trackerMap.isEmpty()) close(r);
            else refreshRemove();
        });
        var previous = trackerMap.put(key, created);
        if (previous != null) previous.close();
        return true;
    }

    private void refreshSpawn() {
        viewedPlayer().forEach(value -> spawnIfNotSpawned(value.player()));
    }

    private void refreshRemove() {
        for (PlayerChannelCache value : viewedPlayerMap.values()) {
            value.hide();
        }
    }

    private void initialLoad() {
        if (ThreadUtil.isFoliaSafe() && loaded.compareAndSet(false, true)) {
            load();
            refreshPlayer();
        }
    }

    private void refreshPlayer() {
        entity.trackedBy()
                .map(p -> BetterModel.player(p.getUniqueId()).orElse(null))
                .filter(Objects::nonNull)
                .forEach(this::registerPlayer);
    }

    /**
     * Removes some tracker in this registry
     * @param key key
     * @return success
     */
    public boolean remove(@NotNull String key) {
        try (var removed = trackerMap.remove(key)) {
            save();
            return removed != null;
        }
    }

    /**
     * Checks this registry is closed
     * @return is closed
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Closes this registry
     * @return success
     */
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
        if (!reason.shouldBeSave()) runSync(() -> entity.modelData(null));
        REGISTRY_LOCK.accessToWriteLock(() -> {
            UUID_REGISTRY_MAP.remove(uuid);
            ID_REGISTRY_MAP.remove(id);
            if (entity instanceof BasePlayer player) player.updateInventory();
            return null;
        });
        LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> uuid + "'s tracker registry has been removed. (" + UUID_REGISTRY_MAP.size() + ")");
        return true;
    }

    /**
     * Reloads this registry
     */
    public void reload() {
        closed.set(true);
        var data = new ArrayList<TrackerData>(trackerMap.size());
        for (EntityTracker value : trackers()) {
            if (!value.canBeSaved()) continue;
            data.add(value.asTrackerData());
            value.close();
        }
        trackerMap.clear();
        closed.set(false);
        load(data.stream());
    }

    /**
     * Refreshes this registry
     */
    public void refresh() {
        if (entity.dead()) return;
        for (EntityTracker value : trackers()) {
            value.refresh();
        }
        refreshPlayer();
        refreshSpawn();
    }

    /**
     * Despawns this registry to all players
     */
    public void despawn() {
        for (EntityTracker value : trackers()) {
            if (!value.forRemoval()) value.despawn();
        }
        viewedPlayerMap.clear();
    }

    /**
     * Loads tracker data in this registry
     * @param stream stream
     */
    public void load(@NotNull Stream<TrackerData> stream) {
        stream.forEach(parsed -> BetterModel.model(parsed.id()).ifPresent(model -> model.create(entity, parsed.modifier(), parsed::applyAs)));
        save();
    }

    /**
     * Loads entity's tracker this in this registry
     */
    public void load() {
        load(deserialize(entity.modelData())
                .stream()
                .map(TrackerData::deserialize));
    }

    /**
     * Saves entity data
     */
    public void save() {
        var data = serialize().toString();
        runSync(() -> entity.modelData(data));
    }

    private void runSync(@NotNull Runnable runnable) {
        if (ThreadUtil.isTickThread()) {
            runnable.run();
        } else BetterModel.plugin().scheduler().task(entity.location(), runnable);
    }

    /**
     * Gets all displays as stream
     * @return all displays
     */
    public @NotNull Stream<ModelDisplay> displays() {
        return trackers()
                .stream()
                .flatMap(Tracker::displays);
    }

    /**
     * Serializes all tracker's data
     * @return tracker's data
     */
    public @NotNull JsonArray serialize() {
        return CollectionUtil.mapToJson(trackers().stream().filter(EntityTracker::canBeSaved), value -> value.asTrackerData().serialize());
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
                .anyMatch(t -> t.isSpawned(uuid));
    }

    /**
     * Spawns this tracker to some player
     * @param player target player
     * @return success
     */
    public boolean spawn(@NotNull Player player) {
        initialLoad();
        return spawn(player, false);
    }
    /**
     * Spawns not spawned tracker to some player
     * @param player target player
     * @return success
     */
    public boolean spawnIfNotSpawned(@NotNull Player player) {
        initialLoad();
        return spawn(player, true);
    }
    private boolean spawn(@NotNull Player player, boolean shouldNotSpawned) {
        var handler = BetterModel.plugin()
                .playerManager()
                .player(player.getUniqueId());
        if (handler == null) return false;
        var cache = registerPlayer(handler);
        if (trackerMap.isEmpty()) return false;
        var bundler = BetterModel.nms().createBundler(10);
        for (EntityTracker value : trackers()) {
            if (shouldNotSpawned && value.isSpawned(player)) continue;
            if (value.canBeSpawnedAt(player)) value.spawn(player, bundler);
        }
        if (bundler.isEmpty()) return false;
        BetterModel.nms().mount(this, bundler);
        cache.spawn(bundler);
        return true;
    }

    private @NotNull PlayerChannelCache registerPlayer(@NotNull PlayerChannelHandler handler) {
        return viewedPlayerMap.computeIfAbsent(handler.uuid(), u -> new PlayerChannelCache(handler));
    }

    /**
     * Gets all viewed player as stream
     * @return viewed player
     */
    public @NotNull Stream<PlayerChannelHandler> viewedPlayer() {
        return viewedPlayerMap.values().stream().map(c -> c.channelHandler);
    }

    /**
     * Removes player from this registry
     * @param player player
     * @return success
     */
    public boolean remove(@NotNull Player player) {
        var cache = viewedPlayerMap.remove(player.getUniqueId());
        if (cache == null) return false;
        var handler = cache.channelHandler;
        handler.sendEntityData(this);
        for (EntityTracker value : trackers()) {
            if (!value.forRemoval() && value.isSpawned(player)) value.remove(handler.player());
        }
        return true;
    }

    /**
     * Gets hide option for some player
     * @param uuid uuid
     * @return hide option
     */
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
     * @param bone bone
     * @param entity entity
     * @param hitBox hitbox
     */
    public record MountedHitBox(@NotNull RenderedBone bone, @NotNull Entity entity, @NotNull HitBox hitBox) {
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
            BetterModel.nms().hide(channelHandler, EntityTrackerRegistry.this);
        }

        private void spawn(@NotNull PacketBundler bundler) {
            reapplyHideOption();
            bundler.send(channelHandler.player(), () -> BetterModel.nms().hide(channelHandler, EntityTrackerRegistry.this, () -> viewedPlayerMap.containsKey(channelHandler.uuid())));
        }

        private synchronized void reapplyHideOption() {
            hideOption = EntityHideOption.composite(trackers()
                    .stream()
                    .filter(t -> t.isSpawned(channelHandler.uuid()))
                    .map(EntityTracker::hideOption));
        }
    }
}
