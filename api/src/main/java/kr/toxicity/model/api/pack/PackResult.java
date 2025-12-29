/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

/**
 * Represents the result of a pack building process.
 * <p>
 * This class holds the generated pack metadata, the output directory, and the collection of generated resources (assets and overlays).
 * It also provides methods to calculate the pack hash and check for changes.
 * </p>
 *
 * @since 1.15.2
 */
@RequiredArgsConstructor
public final class PackResult {
    private final PackMeta meta;
    private final File directory;
    private final Map<PackOverlay, Set<PackByte>> overlays = new TreeMap<>();
    private final Set<PackByte> assets = new TreeSet<>();
    private final Set<PackByte> assetsView = Collections.unmodifiableSet(assets);

    private final long creationTime = System.currentTimeMillis();
    private boolean frozen = false;
    private boolean changed = false;
    private UUID uuid;

    /**
     * Adds a resource to the result.
     *
     * @param overlay the overlay the resource belongs to (or null for base assets)
     * @param packByte the resource data
     * @throws IllegalStateException if the result is frozen
     * @since 1.15.2
     */
    @ApiStatus.Internal
    public void set(@Nullable PackOverlay overlay, @NotNull PackByte packByte) {
        if (frozen) throw new IllegalStateException("result is frozen.");
        if (overlay == null) {
            synchronized (assets) {
                assets.add(packByte);
            }
            return;
        }
        synchronized (overlays) {
            overlays.computeIfAbsent(overlay, o -> new TreeSet<>()).add(packByte);
        }
    }

    /**
     * Freezes the result, preventing further modifications.
     *
     * @since 1.15.2
     */
    public void freeze() {
        freeze(false);
    }

    /**
     * Checks if the pack content has changed.
     *
     * @return true if changed, false otherwise
     * @since 1.15.2
     */
    public boolean changed() {
        return changed;
    }

    /**
     * Freezes the result and sets the changed status.
     *
     * @param changed whether the pack content has changed
     * @throws IllegalStateException if the result is already frozen
     * @since 1.15.2
     */
    public void freeze(boolean changed) {
        if (frozen) throw new IllegalStateException("result is frozen.");
        frozen = true;
        this.changed = changed;
    }

    /**
     * Returns the pack metadata.
     *
     * @return the pack metadata
     * @since 1.15.2
     */
    @NotNull
    public PackMeta meta() {
        return meta;
    }

    /**
     * Returns the output directory of the pack.
     *
     * @return the directory, or null if not applicable
     * @since 1.15.2
     */
    public @Nullable File directory() {
        return directory;
    }

    /**
     * Calculates and returns the SHA-256 hash of the pack content as a UUID.
     *
     * @return the hash UUID
     * @since 1.15.2
     */
    public @NotNull UUID hash() {
        if (uuid != null) return uuid;
        synchronized (this) {
            if (uuid != null) return uuid;
            try {
                var sha = MessageDigest.getInstance("SHA-256");
                stream().map(PackByte::bytes).forEach(sha::update);
                return uuid = UUID.nameUUIDFromBytes(sha.digest());
            } catch (Exception e) {
                return uuid = UUID.randomUUID();
            }
        }
    }

    /**
     * Returns the total number of resources in the pack.
     *
     * @return the size
     * @since 1.15.2
     */
    public int size() {
        return assets.size() + overlays.values().stream().mapToInt(Set::size).sum();
    }

    /**
     * Returns the time elapsed since the result was created.
     *
     * @return the elapsed time in milliseconds
     * @since 1.15.2
     */
    public long time() {
        return System.currentTimeMillis() - creationTime;
    }

    /**
     * Returns the resources for a specific overlay.
     *
     * @param overlay the overlay
     * @return the set of resources
     * @since 1.15.2
     */
    @NotNull
    @Unmodifiable
    public Set<PackByte> overlays(@NotNull PackOverlay overlay) {
        var get = overlays.get(overlay);
        return get != null ? Collections.unmodifiableSet(get) : Collections.emptySet();
    }

    /**
     * Returns a stream of all resources in the pack.
     *
     * @return the stream of resources
     * @since 1.15.2
     */
    public @NotNull Stream<PackByte> stream() {
        return Stream.concat(
            overlays.values().stream().flatMap(Collection::stream),
            assets.stream()
        );
    }

    /**
     * Returns the base assets of the pack.
     *
     * @return the set of assets
     * @since 1.15.2
     */
    @NotNull
    @Unmodifiable
    public Set<PackByte> assets() {
        return assetsView;
    }
}
