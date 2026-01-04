/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
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
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
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
 * Manages all entity trackers for a specific entity.
 * <p>
 * This registry handles the lifecycle of trackers attached to an entity, including loading, saving,
 * spawning, and despawning. It acts as a central hub for accessing and manipulating models on an entity.
 * </p>
 *
 * @since 1.15.2
 */
@ToString(onlyExplicitlyIncluded = true)
public final class EntityTrackerRegistry {

    private static final Object2ReferenceMap<UUID, EntityTrackerRegistry> UUID_REGISTRY_MAP = new Object2ReferenceOpenHashMap<>();
    private static final Int2ReferenceMap<EntityTrackerRegistry> ID_REGISTRY_MAP = new Int2ReferenceOpenHashMap<>();
    private static final DuplexLock REGISTRY_LOCK = new DuplexLock();
    /**
     * The namespaced key used for tracking ID.
     * @since 1.15.2
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
     * Retrieves a registry by entity UUID.
     *
     * @param uuid the entity UUID
     * @return the registry, or null if not found
     * @since 1.15.2
     */
    public static @Nullable EntityTrackerRegistry registry(@NotNull UUID uuid) {
        return REGISTRY_LOCK.accessToReadLock(() -> UUID_REGISTRY_MAP.get(uuid));
    }

    /**
     * Retrieves a registry by entity ID.
     *
     * @param id the entity ID
     * @return the registry, or null if not found
     * @since 1.15.2
     */
    public static @Nullable EntityTrackerRegistry registry(int id) {
        return REGISTRY_LOCK.accessToReadLock(() -> ID_REGISTRY_MAP.get(id));
    }

    /**
     * Retrieves a registry for a base entity.
     *
     * @param entity the base entity
     * @return the registry, or null if the entity has no model data
     * @since 1.15.2
     */
    public static @Nullable EntityTrackerRegistry registry(@NotNull BaseEntity entity) {
        return entity.hasModelData() ? getOrCreate(entity) : null;
    }

    /**
     * Iterates over all active registries.
     *
     * @param consumer the consumer to apply
     * @since 1.15.2
     */
    public static void registries(@NotNull Consumer<EntityTrackerRegistry> consumer) {
        for (EntityTrackerRegistry registry : registries()) {
            consumer.accept(registry);
        }
    }

    /**
     * Returns a list of all active registries.
     *
     * @return the list of registries
     * @since 1.15.2
     */
    public static @NotNull @Unmodifiable List<EntityTrackerRegistry> registries() {
        return REGISTRY_LOCK.accessToReadLock(() -> ImmutableList.copyOf(UUID_REGISTRY_MAP.values()));
    }

    /**
     * Gets or creates a registry for a base entity.
     *
     * @param entity the base entity
     * @return the registry
     * @since 1.15.2
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
     * Returns the source entity.
     *
     * @return the entity
     * @since 1.15.2
     */
    public @NotNull BaseEntity entity() {
        return entity;
    }

    /**
     * Returns the entity UUID.
     *
     * @return the UUID
     * @since 1.15.2
     */
    public @NotNull UUID uuid() {
        return uuid;
    }

    /**
     * Returns the entity ID.
     *
     * @return the ID
     * @since 1.15.2
     */
    public int id() {
        return id;
    }

    /**
     * Returns all trackers in this registry.
     *
     * @return the trackers
     * @since 1.15.2
     */
    public @NotNull @Unmodifiable Collection<EntityTracker> trackers() {
        return trackers;
    }

    /**
     * Retrieves a tracker by key.
     *
     * @param key the key (model ID), or null for the first tracker
     * @return the tracker, or null if not found
     * @since 1.15.2
     */
    public @Nullable EntityTracker tracker(@Nullable String key) {
        return key == null ? first() : trackerMap.get(key);
    }

    /**
     * Returns the first tracker in the registry.
     *
     * @return the first tracker, or null if empty
     * @since 1.15.2
     */
    public @Nullable EntityTracker first() {
        var entry = trackerMap.firstEntry();
        return entry != null ? entry.getValue() : null;
    }

    /**
     * Creates a new tracker in this registry.
     *
     * @param key the key (model ID)
     * @param supplier the supplier to create the tracker
     * @return the created tracker
     * @since 1.15.2
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
     * Gets or creates a tracker in this registry.
     *
     * @param key the key (model ID)
     * @param supplier the supplier to create the tracker
     * @return the tracker
     * @since 1.15.2
     */
    @ApiStatus.Internal
    public @NotNull EntityTracker getOrCreate(@NotNull String key, @NotNull Function<EntityTrackerRegistry, EntityTracker> supplier) {
        var get = trackerMap.get(key);
        return get != null ? get : create(key, supplier);
    }

    private boolean putTracker(@NotNull String key, @NotNull EntityTracker created) {
        if (isClosed() || created.isClosed()) return false;
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
     * Removes a tracker from the registry.
     *
     * @param key the key (model ID)
     * @return true if removed successfully
     * @since 1.15.2
     */
    public boolean remove(@NotNull String key) {
        try (var removed = trackerMap.remove(key)) {
            save();
            return removed != null;
        }
    }

    /**
     * Checks if the registry is closed.
     *
     * @return true if closed
     * @since 1.15.2
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Closes the registry.
     *
     * @return true if closed successfully
     * @since 1.15.2
     */
    public boolean close() {
        return close(Tracker.CloseReason.REMOVE);
    }

    /**
     * Closes the registry with a specific reason.
     *
     * @param reason the close reason
     * @return true if closed successfully
     * @since 1.15.2
     */
    public boolean close(@NotNull Tracker.CloseReason reason) {
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
     * Reloads the registry, refreshing all trackers.
     *
     * @since 1.15.2
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
     * Refreshes the registry state.
     *
     * @since 1.15.2
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
     * Despawns all trackers in the registry.
     *
     * @since 1.15.2
     */
    public void despawn() {
        for (EntityTracker value : trackers()) {
            if (!value.forRemoval()) value.despawn();
        }
        viewedPlayerMap.clear();
    }

    /**
     * Loads trackers from a stream of data.
     *
     * @param stream the data stream
     * @since 1.15.2
     */
    public void load(@NotNull Stream<TrackerData> stream) {
        stream.forEach(parsed -> BetterModel.model(parsed.id()).ifPresent(model -> model.create(entity, parsed.modifier(), parsed::applyAs)));
        save();
    }

    /**
     * Loads trackers from the entity's persistent data.
     *
     * @since 1.15.2
     */
    public void load() {
        load(deserialize(entity.modelData())
                .stream()
                .map(TrackerData::deserialize));
    }

    /**
     * Saves the current tracker state to the entity's persistent data.
     *
     * @since 1.15.2
     */
    public void save() {
        var data = serialize();
        if (!data.isEmpty()) runSync(() -> entity.modelData(data.toString()));
    }

    private void runSync(@NotNull Runnable runnable) {
        if (ThreadUtil.isTickThread()) {
            runnable.run();
        } else BetterModel.plugin().scheduler().task(entity.location(), runnable);
    }

    /**
     * Returns a stream of all displays from all trackers.
     *
     * @return the displays
     * @since 1.15.2
     */
    public @NotNull Stream<ModelDisplay> displays() {
        return trackers()
                .stream()
                .flatMap(Tracker::displays);
    }

    /**
     * Serializes the registry state to a JSON array.
     *
     * @return the JSON array
     * @since 1.15.2
     */
    public @NotNull JsonArray serialize() {
        return CollectionUtil.mapToJson(trackers().stream().filter(EntityTracker::canBeSaved), value -> value.asTrackerData().serialize());
    }

    /**
     * Checks if any tracker is spawned for a player.
     *
     * @param player the player
     * @return true if spawned
     * @since 1.15.2
     */
    public boolean isSpawned(@NotNull Player player) {
        return isSpawned(player.getUniqueId());
    }
    /**
     * Checks if any tracker is spawned for a player UUID.
     *
     * @param uuid the player UUID
     * @return true if spawned
     * @since 1.15.2
     */
    public boolean isSpawned(@NotNull UUID uuid) {
        return viewedPlayerMap.containsKey(uuid) && trackers()
                .stream()
                .anyMatch(t -> t.isSpawned(uuid));
    }

    /**
     * Spawns trackers for a player.
     *
     * @param player the player
     * @return true if spawned successfully
     * @since 1.15.2
     */
    public boolean spawn(@NotNull Player player) {
        initialLoad();
        return spawn(player, false);
    }
    /**
     * Spawns trackers for a player only if not already spawned.
     *
     * @param player the player
     * @return true if spawned successfully
     * @since 1.15.2
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
     * Returns a stream of all players viewing this registry.
     *
     * @return the players
     * @since 1.15.2
     */
    public @NotNull Stream<PlayerChannelHandler> viewedPlayer() {
        return viewedPlayerMap.values().stream().map(c -> c.channelHandler);
    }

    /**
     * Removes a player from viewing this registry.
     *
     * @param player the player
     * @return true if removed successfully
     * @since 1.15.2
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
     * Returns the hide option for a specific player.
     *
     * @param uuid the player UUID
     * @return the hide option
     * @since 1.15.2
     */
    public @NotNull EntityHideOption hideOption(@NotNull UUID uuid) {
        var cache = viewedPlayerMap.get(uuid);
        return cache != null ? cache.hideOption : EntityHideOption.FALSE;
    }

    /**
     * Returns the map of currently mounted hitboxes.
     *
     * @return the mounted hitboxes
     * @since 1.15.2
     */
    @NotNull
    @Unmodifiable
    public Map<UUID, MountedHitBox> mountedHitBox() {
        return mountedHitBox;
    }

    /**
     * Checks if any hitbox has a passenger.
     *
     * @return true if there is a passenger
     * @since 1.15.2
     */
    public boolean hasPassenger() {
        return !mountedHitBox().isEmpty();
    }

    /**
     * Checks if any hitbox has a controlling passenger.
     *
     * @return true if there is a controlling passenger
     * @since 1.15.2
     */
    public boolean hasControllingPassenger() {
        return mountedHitBox()
                .values()
                .stream()
                .map(MountedHitBox::hitBox)
                .anyMatch(HitBox::hasBeenControlled);
    }


    /**
     * Represents a hitbox that has an entity mounted on it.
     *
     * @param bone the bone associated with the hitbox
     * @param entity the mounted entity
     * @param hitBox the hitbox itself
     * @since 1.15.2
     */
    public record MountedHitBox(@NotNull RenderedBone bone, @NotNull Entity entity, @NotNull HitBox hitBox) {
        /**
         * Dismounts the entity from the hitbox.
         * @since 1.15.2
         */
        public void dismount() {
            hitBox.dismount(entity);
        }
        /**
         * Dismounts all entities from the hitbox.
         * @since 1.15.2
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
