package kr.toxicity.model.api.nms;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface HitBox {
    @NotNull String name();
    @NotNull Entity source();
    @NotNull Entity entity();
    @NotNull Vector3f relativePosition();
    void remove();
    int id();
}
