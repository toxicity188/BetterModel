package kr.toxicity.model.api.tracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.nms.EntityAdapter;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class EntityTrackerRegistry {

    private static final Map<UUID, EntityTrackerRegistry> REGISTRY_MAP = new ConcurrentHashMap<>();
    private static final Int2ObjectMap<EntityTrackerRegistry> ID_TRACKER_MAP = new Int2ObjectOpenHashMap<>();
    /**
     * Tracker's namespace.
     */
    public static final NamespacedKey TRACKING_ID = Objects.requireNonNull(NamespacedKey.fromString("bettermodel_tracker"));
    /**
     * Registries
     */
    public static final Collection<EntityTrackerRegistry> REGISTRIES = Collections.unmodifiableCollection(REGISTRY_MAP.values());

    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean autoSpawn = new AtomicBoolean(true);
    private final Entity entity;
    private final int id;
    private final EntityAdapter adapter;
    private final ConcurrentNavigableMap<String, EntityTracker> trackerMap = new ConcurrentSkipListMap<>();
    private final Collection<EntityTracker> trackers = Collections.unmodifiableCollection(trackerMap.values());
    private final Map<UUID, PlayerChannelHandler> viewedPlayerMap = new ConcurrentHashMap<>();
    private Predicate<Player> spawnFilter = p -> autoSpawn() && p.getWorld() == entity().getWorld();
    private HideOption hideOption = HideOption.DEFAULT;

    public static @Nullable EntityTrackerRegistry registry(@NotNull UUID uuid) {
        return REGISTRY_MAP.get(uuid);
    }

    public static @Nullable EntityTrackerRegistry registry(int id) {
        return ID_TRACKER_MAP.get(id);
    }

    @ApiStatus.Internal
    public static @NotNull EntityTrackerRegistry registry(@NotNull Entity entity) {
        var get = registry(entity.getUniqueId());
        if (get != null) return get;
        var registry = new EntityTrackerRegistry(entity);
        var put = REGISTRY_MAP.putIfAbsent(entity.getUniqueId(), registry);
        if (put != null) return put;
        synchronized (ID_TRACKER_MAP) {
            ID_TRACKER_MAP.put(registry.id, registry);
        }
        registry.load();
        entity.getTrackedBy()
                .stream()
                .map(p -> BetterModel.player(p.getUniqueId()).orElse(null))
                .filter(Objects::nonNull)
                .forEach(h -> registry.viewedPlayerMap.put(h.player().getUniqueId(), h));
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
        this.id = entity.getEntityId();
        this.adapter = BetterModel.plugin().nms().adapt(entity);
    }

    public @NotNull Entity entity() {
        return entity;
    }

    public @NotNull UUID uuid() {
        return entity().getUniqueId();
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
        created.handleCloseEvent(t -> {
            if (isClosed()) return;
            trackerMap.compute(key, (k, v) -> v == created ? null : v);
            if (trackerMap.isEmpty()) close0();
        });
        var previous = trackerMap.put(key, created);
        if (previous != null) previous.close();
        return true;
    }

    private void refreshSpawn() {
        for (PlayerChannelHandler value : viewedPlayerMap.values()) {
            spawn(value.player());
        }
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
        if (closed.compareAndSet(false, true)) {
            close0();
            return true;
        } else return false;
    }

    private void close0() {
        for (PlayerChannelHandler value : viewedPlayerMap.values()) {
            value.sendEntityData(this);
        }
        viewedPlayerMap.clear();
        for (EntityTracker value : trackerMap.values()) {
            value.close();
        }
        entity.getPersistentDataContainer().remove(TRACKING_ID);
        if (REGISTRY_MAP.remove(entity.getUniqueId()) != null) {
            synchronized (ID_TRACKER_MAP) {
                ID_TRACKER_MAP.remove(id);
            }
        }
    }

    public void reload() {
        closed.set(true);
        var data = new ArrayList<TrackerData>(trackerMap.size());
        for (EntityTracker value : trackerMap.values()) {
            data.add(value.asTrackerData());
            value.close();
        }
        trackerMap.clear();
        closed.set(false);
        load(data.stream());
    }

    public void refresh() {
        if (adapter.dead()) return;
        for (EntityTracker value : trackerMap.values()) {
            value.refresh();
        }
    }

    public void despawn() {
        for (EntityTracker value : trackerMap.values()) {
            if (!value.forRemoval()) value.despawn();
        }
    }

    public void load(@NotNull Stream<TrackerData> stream) {
        stream.forEach(parsed -> BetterModel.model(parsed.id()).ifPresent(model -> model.create(entity, parsed.modifier(), t -> {
            t.scaler(parsed.scaler());
            t.rotator(parsed.rotator());
        })));
        save();
    }

    public void load() {
        load(deserialize(entity.getPersistentDataContainer().get(TRACKING_ID, PersistentDataType.STRING))
                .stream()
                .map(TrackerData::deserialize));
    }

    public void save() {
        entity.getPersistentDataContainer().set(TRACKING_ID, PersistentDataType.STRING, serialize().toString());
    }

    public @NotNull Stream<ModelDisplay> displays() {
        return trackerMap.values()
                .stream()
                .flatMap(Tracker::displays);
    }

    public @NotNull JsonArray serialize() {
        var array = new JsonArray(trackerMap.size());
        for (EntityTracker value : trackerMap.values()) {
            array.add(value.asTrackerData().serialize());
        }
        return array;
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
     * Checks this tracker can be spawned at this player's client
     * @param player target player
     * @return can be spawned at
     */
    public boolean canBeSpawnedAt(@NotNull Player player) {
        return spawnFilter.test(player);
    }

    public void spawnFilter(@NotNull Predicate<Player> predicate) {
        this.spawnFilter = this.spawnFilter.and(predicate);
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
        return viewedPlayerMap.containsKey(uuid);
    }

    /**
     * Spawns this tracker to player if the spawn condition is matched
     * @param player player
     * @return success
     */
    public boolean spawnIfMatched(@NotNull Player player) {
        return canBeSpawnedAt(player) && spawn(player);
    }

    /**
     * Spawns this tracker to some player
     * @param player target player
     * @return success
     */
    public boolean spawn(@NotNull Player player) {
        var handler = BetterModel.plugin()
                .playerManager()
                .player(player.getUniqueId());
        if (handler == null) return false;
        viewedPlayerMap.put(player.getUniqueId(), handler);
        if (trackerMap.isEmpty()) return false;
        var bundler = BetterModel.plugin().nms().createBundler(10);
        for (EntityTracker value : trackerMap.values()) {
            value.spawn(player, bundler);
        }
        BetterModel.plugin().nms().mount(this, bundler);
        bundler.send(player, () -> BetterModel.plugin().nms().hide(player, this, () -> viewedPlayerMap.containsKey(player.getUniqueId())));
        return true;
    }

    public boolean remove(@NotNull Player player) {
        var handler = viewedPlayerMap.remove(player.getUniqueId());
        if (handler == null) return false;
        handler.sendEntityData(this);
        for (EntityTracker value : trackerMap.values()) {
            if (!value.forRemoval()) value.remove(handler.player());
        }
        return true;
    }

    public @NotNull HideOption hideOption() {
        return hideOption;
    }

    public void hideOption(@NotNull HideOption hideOption) {
        this.hideOption = Objects.requireNonNull(hideOption);
    }

    @lombok.Builder
    public record HideOption(
            boolean equipment,
            boolean fire,
            boolean visibility,
            boolean glowing
    ) {
        public static final HideOption DEFAULT = new HideOption(
                true,
                true,
                true,
                true
        );
    }
}
