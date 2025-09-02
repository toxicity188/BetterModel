package kr.toxicity.model.api.pack;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@RequiredArgsConstructor
public final class PackResult {
    private final PackMeta meta;
    private final File directory;
    private final Map<PackOverlay, Set<PackByte>> overlays = new TreeMap<>();
    private final Set<PackByte> assets = ConcurrentHashMap.newKeySet();
    private final Set<PackByte> assetsView = Collections.unmodifiableSet(assets);

    private final long creationTime = System.currentTimeMillis();
    private boolean frozen = false;
    private boolean changed = false;
    private long time;
    private volatile UUID uuid;

    @ApiStatus.Internal
    public void set(@Nullable PackOverlay overlay, @NotNull PackByte packByte) {
        if (frozen) throw new IllegalStateException("result is frozen.");
        if (overlay == null) {
            assets.add(packByte);
            return;
        }
        synchronized (overlays) {
            overlays.computeIfAbsent(overlay, o -> new TreeSet<>()).add(packByte);
        }
    }

    public void freeze() {
        freeze(false);
    }

    public boolean changed() {
        return changed;
    }

    public void freeze(boolean changed) {
        if (frozen) throw new IllegalStateException("result is frozen.");
        frozen = true;
        this.changed = changed;
        time = System.currentTimeMillis() - creationTime;
    }

    @NotNull
    public PackMeta meta() {
        return meta;
    }

    public @Nullable File directory() {
        return directory;
    }

    public @NotNull UUID hash() {
        if (uuid != null) return uuid;
        synchronized (this) {
            if (uuid != null) return uuid;
            try {
                var sha = MessageDigest.getInstance("SHA-256");
                for (Set<PackByte> value : overlays.values()) {
                    for (PackByte packByte : value) {
                        sha.update(packByte.bytes());
                    }
                }
                return uuid = UUID.nameUUIDFromBytes(sha.digest());
            } catch (Exception e) {
                return uuid = UUID.randomUUID();
            }
        }
    }

    public int size() {
        return assets.size() + overlays.values().stream().mapToInt(Set::size).sum();
    }

    public long time() {
        return time;
    }

    @NotNull
    @Unmodifiable
    public Set<PackByte> overlays(@NotNull PackOverlay overlay) {
        var get = overlays.get(overlay);
        return get != null ? Collections.unmodifiableSet(get) : Collections.emptySet();
    }

    public Stream<PackByte> stream() {
        return Stream.concat(
                overlays.values().stream().flatMap(Collection::stream),
                assets.stream()
        );
    }

    @NotNull
    @Unmodifiable
    public Set<PackByte> assets() {
        return assetsView;
    }
}
