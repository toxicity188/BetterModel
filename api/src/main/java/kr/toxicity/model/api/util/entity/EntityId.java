package kr.toxicity.model.api.util.entity;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record EntityId(@NotNull UUID worldId, int entityId) {
    public static @NotNull EntityId of(@NotNull Entity entity) {
        return new EntityId(entity.getWorld().getUID(), entity.getEntityId());
    }
}
