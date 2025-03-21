package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.mount.MountController;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface HitBox {
    @NotNull String name();
    @NotNull MountController mountController();
    boolean onWalk();
    @NotNull Entity source();
    void addPassenger(@NotNull Entity entity);
    @NotNull Vector3f relativePosition();
    void remove();
    int id();
    @NotNull HitBoxListener listener();
}
