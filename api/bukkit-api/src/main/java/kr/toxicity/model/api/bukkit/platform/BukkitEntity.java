/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.platform;

import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.platform.PlatformLocation;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a Bukkit entity wrapped as a {@link PlatformEntity}.
 *
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode
public class BukkitEntity implements PlatformEntity {

    private final Entity source;

    /**
     * Creates a new BukkitEntity wrapper.
     *
     * @param source the source Bukkit entity
     * @since 2.0.0
     */
    public BukkitEntity(@NotNull Entity source) {
        this.source = source;
    }

    /**
     * Returns the underlying Bukkit entity.
     *
     * @return the source entity
     * @since 2.0.0
     */
    public Entity source() {
        return source;
    }

    @Override
    public @NotNull UUID uuid() {
        return source.getUniqueId();
    }

    @Override
    public @NotNull PlatformLocation location() {
        return BukkitAdapter.adapt(source.getLocation());
    }
}
