/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.scheduler.ModelTask;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents an entity in the underlying platform.
 * <p>
 * This interface provides access to basic entity properties like UUID and location,
 * as well as integration with the {@link EntityTrackerRegistry}.
 * </p>
 *
 * @since 2.0.0
 */
public interface PlatformEntity extends PlatformRegionHolder {

    /**
     * Returns the unique identifier of the entity.
     *
     * @return the UUID
     * @since 2.0.0
     */
    @NotNull UUID uuid();

    /**
     * Returns the current location of the entity.
     *
     * @return the location
     * @since 2.0.0
     */
    @NotNull PlatformLocation location();

    @Override
    default @Nullable ModelTask task(@NotNull Runnable runnable) {
        return location().task(runnable);
    }

    @Override
    default @Nullable ModelTask taskLater(long delay, @NotNull Runnable runnable) {
        return location().taskLater(delay, runnable);
    }

    /**
     * Retrieves the tracker registry associated with this entity.
     *
     * @return an optional containing the registry if it exists
     * @since 2.0.0
     */
    default @NotNull Optional<EntityTrackerRegistry> registry() {
        return BetterModel.registry(uuid());
    }

    /**
     * Retrieves a specific tracker by name from this entity's registry.
     *
     * @param name the name of the tracker
     * @return an optional containing the tracker if found
     * @since 2.0.0
     */
    default @NotNull Optional<EntityTracker> tracker(@NotNull String name) {
        return registry().map(registry -> registry.tracker(name));
    }
}
