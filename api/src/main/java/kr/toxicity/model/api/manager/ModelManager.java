package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.data.renderer.BlueprintRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Model Manager
 */
public interface ModelManager extends GlobalManager {

    /**
     * Gets renderer by name
     * @param name name
     * @return renderer or null
     */
    @Nullable BlueprintRenderer renderer(@NotNull String name);

    /**
     * Gets all renderers
     * @return all renderers
     */
    @NotNull @Unmodifiable
    List<BlueprintRenderer> renderers();

    /**
     * Gets all key of renderer
     * @return keys
     */
    @NotNull @Unmodifiable
    Set<String> keys();

    /**
     * Get or creates tracker of source entity
     * @param entity entity
     * @return tracker or null
     */
    default @Nullable EntityTracker tracker(@NotNull Entity entity) {
        return EntityTracker.tracker(entity);
    }

    /**
     * Gets tracker of source entity
     * @param uuid entity's uuid
     * @return tracker or null
     */
    default @Nullable EntityTracker tracker(@NotNull UUID uuid) {
        return EntityTracker.tracker(uuid);
    }
}
