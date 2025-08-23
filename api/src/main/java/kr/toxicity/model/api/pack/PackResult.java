package kr.toxicity.model.api.pack;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public final class PackResult {
    private final PackMeta meta;
    private final File directory;
    private final Map<PackOverlay, Set<PackByte>> overlays = new EnumMap<>(PackOverlay.class);
    private final Set<PackByte> assets = ConcurrentHashMap.newKeySet();
    private final Set<PackByte> assetsView = Collections.unmodifiableSet(assets);

    private final long creationTime = System.currentTimeMillis();
    private boolean frozen = false;
    private long time;

    @ApiStatus.Internal
    public void set(@Nullable PackOverlay overlay, @NotNull PackByte packByte) {
        if (frozen) throw new IllegalStateException("result is frozen.");
        if (overlay == null) {
            assets.add(packByte);
            return;
        }
        synchronized (overlays) {
            overlays.computeIfAbsent(overlay, o -> new HashSet<>()).add(packByte);
        }
    }

    public void freeze() {
        if (frozen) throw new IllegalStateException("result is frozen.");
        frozen = true;
        time = System.currentTimeMillis() - creationTime;
    }

    @NotNull
    public PackMeta meta() {
        return meta;
    }

    public @Nullable File directory() {
        return directory;
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

    @NotNull
    @Unmodifiable
    public Set<PackByte> assets() {
        return assetsView;
    }
}
