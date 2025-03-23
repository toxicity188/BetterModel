package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.mount.MountController;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface HitBox {
    @NotNull String groupName();
    @NotNull MountController mountController();
    void mountController(@NotNull MountController controller);
    boolean onWalk();
    @NotNull Entity source();
    void mount(@NotNull Entity entity);
    void dismount(@NotNull Entity entity);
    boolean forceDismount();
    @NotNull Vector3f relativePosition();
    void removeHitBox();
    @NotNull HitBoxListener listener();
}
