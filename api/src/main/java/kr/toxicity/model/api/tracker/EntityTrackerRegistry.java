package kr.toxicity.model.api.tracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.config.DebugConfig;
import kr.toxicity.model.api.nms.EntityAdapter;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.util.LogUtil;
import kr.toxicity.model.api.util.entity.EntityId;
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
import java.util.stream.Stream;

public final class EntityTrackerRegistry {

    private static final Map<UUID, EntityTrackerRegistry> UUID_REGISTRY_MAP = new ConcurrentHashMap<>();
    private static final Map<EntityId, EntityTrackerRegistry> ID_REGISTRY_MAP = new ConcurrentHashMap<>();
    /**
     * Tracker's namespace.
     */
    public static final NamespacedKey TRACKING_ID = Objects.requireNonNull(NamespacedKey.fromString("bettermodel_tracker"));
    /**
     * Registries
     */
    public static final Collection<EntityTrackerRegistry> REGISTRIES = Collections.unmodifiableCollection(UUID_REGISTRY_MAP.values());

    private final AtomicBoolean closed = new AtomicBoolean();
    private final Entity entity;
    private final EntityId id;
    private final EntityAdapter adapter;
    private final ConcurrentNavigableMap<String, EntityTracker> trackerMap = new ConcurrentSkipListMap<>();
    private final Collection<EntityTracker> trackers = Collections.unmodifiableCollection(trackerMap.values());
    private final Map<UUID, PlayerChannelHandler> viewedPlayerMap = new ConcurrentHashMap<>();
    private HideOption hideOption = HideOption.DEFAULT;

    public static @Nullable EntityTrackerRegistry registry(@NotNull UUID uuid) {
        return UUID_REGISTRY_MAP.get(uuid);
    }

    public static @Nullable EntityTrackerRegistry registry(@NotNull EntityId id) {
        return ID_REGISTRY_MAP.get(id);
    }

    @ApiStatus.Internal
    public static @NotNull EntityTrackerRegistry registry(@NotNull Entity entity) {
        var get = registry(entity.getUniqueId());
        if (get != null) return get;
        var registry = new EntityTrackerRegistry(entity);
        var put = UUID_REGISTRY_MAP.putIfAbsent(entity.getUniqueId(), registry);
        if (put != null) return put;
        ID_REGISTRY_MAP.put(registry.id, registry);
        registry.load();
        var stream = registry.adapter.trackedPlayer().stream();
        if (entity instanceof Player player) stream = Stream.concat(Stream.of(player), stream);
        stream.map(p -> BetterModel.player(p.getUniqueId()).orElse(null))
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
        this.adapter = BetterModel.plugin().nms().adapt(entity);
        this.id = new EntityId(entity.getWorld().getUID(), adapter.id());
    }

    public @NotNull Entity entity() {
        return entity;
    }

    public @NotNull UUID uuid() {
        return entity().getUniqueId();
    }

    public @NotNull EntityId id() {
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
                LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> entity.getUniqueId() + "'s tracker " + key + " has been removed. (" + trackerMap.size() + ")");
            }
            if (trackerMap.isEmpty()) {
                close(r);
                LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> entity.getUniqueId() + "'s tracker registry has been removed. (" + REGISTRIES.size() + ")");
            }
        });
        var previous = trackerMap.put(key, created);
        if (previous != null) previous.close();
        return true;
    }

    public void refreshSpawn() {
        for (PlayerChannelHandler value : viewedPlayerMap.values()) {
            spawn(value.player(), true);
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
            close(Tracker.CloseReason.REMOVE);
            return true;
        } else return false;
    }

    private void close(@NotNull Tracker.CloseReason reason) {
        for (PlayerChannelHandler value : viewedPlayerMap.values()) {
            value.sendEntityData(this);
        }
        viewedPlayerMap.clear();
        for (EntityTracker value : trackerMap.values()) {
            value.close(reason);
        }
        if (!reason.shouldBeSave()) entity.getPersistentDataContainer().remove(TRACKING_ID);
        if (UUID_REGISTRY_MAP.remove(entity.getUniqueId()) != null) {
            ID_REGISTRY_MAP.remove(id);
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
        return viewedPlayerMap.containsKey(uuid) && trackerMap.values()
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
        viewedPlayerMap.put(player.getUniqueId(), handler);
        if (trackerMap.isEmpty()) return false;
        var bundler = BetterModel.plugin().nms().createBundler(10);
        for (EntityTracker value : trackerMap.values()) {
            if (shouldNotSpawned && value.pipeline.isSpawned(player.getUniqueId())) continue;
            if (value.canBeSpawnedAt(player)) value.spawn(player, bundler);
        }
        if (bundler.isEmpty()) return false;
        BetterModel.plugin().nms().mount(this, bundler);
        bundler.send(player, () -> BetterModel.plugin().nms().hide(player, this, () -> viewedPlayerMap.containsKey(player.getUniqueId())));
        return true;
    }


    public boolean remove(@NotNull Player player) {
        var handler = viewedPlayerMap.remove(player.getUniqueId());
        if (handler == null) return false;
        handler.sendEntityData(this);
        for (EntityTracker value : trackerMap.values()) {
            if (!value.forRemoval() && value.pipeline.isSpawned(player.getUniqueId())) value.remove(handler.player());
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
