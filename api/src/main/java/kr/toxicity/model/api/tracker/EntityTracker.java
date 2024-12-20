package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.entity.TrackerMovement;
import kr.toxicity.model.api.nms.ModelDisplay;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

@Getter
public class EntityTracker extends Tracker {
    private final @NotNull Entity entity;

    public EntityTracker(@NotNull Entity entity, @NotNull RenderInstance instance) {
        super(() -> new TrackerMovement(
                new Vector3f(0, entity instanceof LivingEntity livingEntity ? (float) -livingEntity.getEyeHeight() - 0.1F : 0, 0F),
                new Vector3f(1),
                new Vector3f(0, entity instanceof LivingEntity livingEntity ? -livingEntity.getBodyYaw() : -entity.getYaw(), 0)
        ), instance);
        this.entity = entity;
        if (entity instanceof LivingEntity livingEntity) {
            instance.addAnimationMovementModifier(
                    r -> r.getName().startsWith("h_"),
                    a -> {
                        if (a.rotation() != null) {
                            a.rotation().add(-entity.getPitch(), Math.clamp(
                                    -livingEntity.getYaw() + livingEntity.getBodyYaw(),
                                    -45,
                                    45
                            ), 0);
                        }
                    });
            instance.animateLoop("walk", () -> {
                double speed = Optional.ofNullable(livingEntity.getAttribute(Attribute.MOVEMENT_SPEED))
                        .map(AttributeInstance::getValue)
                        .orElse(0.2);
                return entity.isOnGround() && entity.getVelocity().length() / speed > 0.4;
            });
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        entity.remove();
    }

    @Override
    public @NotNull Location location() {
        return entity.getLocation();
    }

    @Override
    public void spawn(@NotNull Player player) {
        super.spawn(player);
        ModelRenderer.inst().playerManager().player(player).startTrack(this);
    }

    public @NotNull List<ModelDisplay> renderers() {
        return instance.renderers();
    }

    @Override
    public void remove(@NotNull Player player) {
        super.remove(player);
        ModelRenderer.inst().playerManager().player(player).endTrack(this);
    }
}
