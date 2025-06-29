package kr.toxicity.model.api.util.entity;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Entity id with world uuid
 * @param worldId world uuid
 * @param entityId entity id
 */
public record EntityId(@NotNull UUID worldId, int entityId) {
    /**
     * Gets from entity
     * @param entity target entity
     * @return entity id
     */
    public static @NotNull EntityId of(@NotNull Entity entity) {
        return new EntityId(entity.getWorld().getUID(), entity.getEntityId());
    }
}
