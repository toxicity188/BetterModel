package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.BoneTags;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.event.CreateEntityTrackerEvent;
import kr.toxicity.model.api.nms.HitBoxListener;
import kr.toxicity.model.api.util.*;
import kr.toxicity.model.api.util.function.BonePredicate;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Entity tracker
 */
public class EntityTracker extends Tracker {
    private final EntityTrackerRegistry registry;

    private final AtomicInteger damageTintValue = new AtomicInteger(0xFF7979);
    private final AtomicLong damageTint = new AtomicLong(-1);

    /**
     * Get or creates tracker
     * @param entity source
     * @return tracker or null
     * @deprecated use EntityTrackerRegistry#registry instead.
     */
    @Deprecated(forRemoval = true)
    public static @Nullable EntityTracker tracker(@NotNull Entity entity) {
        return tracker(entity.getUniqueId());
    }

    /**
     * Gets tracker
     * @param uuid entity's uuid
     * @return tracker or null
     * @deprecated use EntityTrackerRegistry#registry instead.
     */
    @Deprecated(forRemoval = true)
    public static @Nullable EntityTracker tracker(@NotNull UUID uuid) {
        return Optional.ofNullable(EntityTrackerRegistry.registry(uuid))
                .map(EntityTrackerRegistry::first)
                .orElse(null);
    }

    /**
     * Gets world uuid in tracker
     * @return world
     */
    public @NotNull UUID world() {
        return registry.entity().getWorld().getUID();
    }

    /**
     * Creates entity tracker
     * @param instance render instance
     * @param modifier modifier
     */
    @ApiStatus.Internal
    public EntityTracker(@NotNull EntityTrackerRegistry registry, @NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        super(instance, modifier);
        this.registry = registry;

        var entity = registry.entity();
        var adapter = registry.adapter();
        var scale = FunctionUtil.throttleTickFloat(() -> modifier.scale().scale(this));
        //Shadow
        if (modifier.shadow()) {
            var shadow = BetterModel.plugin().nms().create(entity.getLocation());
            var baseScale = (float) instance.bones()
                    .stream()
                    .filter(b -> b.getGroup().getParent().visibility())
                    .map(b -> b.getGroup().getHitBox())
                    .filter(Objects::nonNull)
                    .mapToDouble(b -> Math.max(b.box().x(), b.box().z()))
                    .max()
                    .orElse(0D);
            tick(((t, b) -> {
                shadow.shadowRadius(scale.getAsFloat() * baseScale);
                shadow.sync(adapter);
                shadow.sendEntityData(b);
                shadow.syncPosition(adapter, b);
            }));
            instance.spawnPacketHandler(shadow::spawn);
            instance.despawnPacketHandler(shadow::remove);
            instance.hidePacketHandler(b -> shadow.sendEntityData(false, b));
            instance.showPacketHandler(shadow::sendEntityData);
        }

        //Animation
        instance.defaultPosition(FunctionUtil.throttleTick(() -> adapter.passengerPosition().mul(-1)));
        instance.scale(scale);
        Function<Quaternionf, Quaternionf> headRotator = r -> r.mul(MathUtil.toQuaternion(new Vector3f(
                Math.clamp(adapter.pitch(), -90, 90),
                Math.clamp(-adapter.yaw() + adapter.bodyYaw(), -90, 90),
                0
        )));
        instance.addRotationModifier(
                BonePredicate.of(BonePredicate.State.NOT_SET, r -> r.getName().tagged(BoneTags.HEAD)),
                headRotator
        );
        instance.addRotationModifier(
                BonePredicate.of(BonePredicate.State.TRUE, r -> r.getName().tagged(BoneTags.HEAD_WITH_CHILDREN)),
                headRotator
        );

        var damageTickProvider = FunctionUtil.throttleTickFloat(adapter::damageTick);
        var walkSupplier = FunctionUtil.throttleTickBoolean(() -> adapter.onWalk() || damageTickProvider.getAsFloat() > 0.25 || instance.bones().stream().anyMatch(e -> {
            var hitBox = e.getHitBox();
            return hitBox != null && hitBox.onWalk();
        }));
        var walkSpeedSupplier = FunctionUtil.throttleTickFloat(modifier.damageAnimation() ? () -> adapter.walkSpeed() + 4F * (float) Math.sqrt(damageTickProvider.getAsFloat()) : () -> 1F);
        instance.animate("walk", new AnimationModifier(walkSupplier, 6, 0, AnimationIterator.Type.LOOP, walkSpeedSupplier));
        instance.animate("idle_fly", new AnimationModifier(adapter::fly, 6, 0, AnimationIterator.Type.LOOP, 1F));
        instance.animate("walk_fly", new AnimationModifier(() -> adapter.fly() && walkSupplier.getAsBoolean(), 6, 0, AnimationIterator.Type.LOOP, walkSpeedSupplier));
        instance.animate("spawn", AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
        BetterModel.plugin().scheduler().task(entity, () -> {
            if (isClosed()) return;
            createHitBox();
        });
        tick((t, b) -> updateBaseEntity0());
        tick((t, b) -> {
            var reader = t.instance.getScriptProcessor().getCurrentReader();
            if (reader == null) return;
            var script = reader.script();
            if (script == null) return;
            BetterModel.plugin().scheduler().task(entity, () -> script.accept(entity));
        });
        tick(2, (t, b) -> {
            if (adapter.dead() && !forRemoval()) close();
        });
        frame((t, b) -> {
            if (damageTint.getAndDecrement() == 0) tint(0xFFFFFF);
        });
        rotation(() -> adapter.dead() ? instance.getRotation() : new ModelRotation(0, entity instanceof LivingEntity ? adapter.bodyYaw() : entity.getYaw()));
        update();
        EventUtil.call(new CreateEntityTrackerEvent(this));
    }

    private void createHitBox() {
        createHitBox(e ->
                e.getName().name().equals("hitbox")
                        || e.getName().tagged(BoneTags.HITBOX)
                        || e.getGroup().getMountController().canMount()
        );
    }

    private void createHitBox(@NotNull Predicate<RenderedBone> predicate) {
        createHitBox(predicate, HitBoxListener.EMPTY);
    }

    /**
     * Syncs this tracker to base entity's data.
     */
    public void updateBaseEntity() {
        BetterModel.plugin().scheduler().taskLater(1, registry.entity(), () -> {
            updateBaseEntity0();
            forceUpdate(true);
        });
    }

    /**
     * Updates base entity's data to parent entity
     */
    private void updateBaseEntity0() {
        displays().forEach(d -> d.sync(registry.adapter()));
    }

    /**
     * Gets registry.
     * @return registry
     */
    public @NotNull EntityTrackerRegistry registry() {
        return registry;
    }

    /**
     * Creates hit-box
     * @param predicate predicate
     * @param listener listener
     */
    public void createHitBox(@NotNull Predicate<RenderedBone> predicate, @NotNull HitBoxListener listener) {
        instance.createHitBox(registry.adapter(), predicate, listener);
    }

    /**
     * Gets damage tint value
     * @return value
     */
    public int damageTintValue() {
        return damageTintValue.get();
    }

    /**
     * Sets damage tint value
     * @param tint hex color
     */
    public void damageTintValue(int tint) {
        damageTintValue.set(tint);
    }

    /**
     * Applies damage tint
     */
    public void damageTint() {
        if (!modifier().damageTint()) return;
        var get = damageTint.get();
        if (get <= 0 && damageTint.compareAndSet(get, 50)) tint(damageTintValue.get());
    }

    @Override
    public void close() {
        super.close();
        if (registry.entity() instanceof Player player) player.updateInventory();
    }

    @Override
    public void despawn() {
        if (registry.adapter().dead()) {
            close();
            return;
        }
        super.despawn();
    }

    @Override
    public @NotNull Location location() {
        return registry.entity().getLocation();
    }

    @Override
    public @NotNull UUID uuid() {
        return registry.entity().getUniqueId();
    }

    /**
     * Gets source entity
     * @return source
     */
    public @NotNull Entity sourceEntity() {
        return registry.entity();
    }

    /**
     * Sets move duration of this model.
     * @param duration duration
     */
    public void moveDuration(int duration) {
        instance.moveDuration(duration);
        forceUpdate(true);
    }

    /**
     * Refresh this tracker
     */
    @ApiStatus.Internal
    public void refresh() {
        BetterModel.plugin().scheduler().task(registry.entity(), () -> instance.createHitBox(registry.adapter(), r -> r.getHitBox() != null, null));
    }

    @Override
    public boolean hide(@NotNull Player player) {
        var success = super.hide(player);
        if (success) BetterModel.plugin().scheduler().task(player, () -> player.hideEntity((Plugin) BetterModel.plugin(), registry.entity()));
        return success;
    }

    @Override
    public boolean show(@NotNull Player player) {
        var success = super.show(player);
        if (success) BetterModel.plugin().scheduler().task(player, () -> player.showEntity((Plugin) BetterModel.plugin(), registry.entity()));
        return success;
    }
}
