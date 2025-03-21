package kr.toxicity.model.api.mount;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface MountController {
    @NotNull Vector3f move(@NotNull Player player, @NotNull LivingEntity entity, @NotNull Vector3f input, @NotNull Vector3f travelVector);
    default boolean canMount() {
        return true;
    }
    default boolean canControl() {
        return true;
    }
    default boolean canJump() {
        return true;
    }
    default boolean canFly() {
        return false;
    }
}
