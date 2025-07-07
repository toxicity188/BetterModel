package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.BoneTags;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.event.CreateEntityTrackerEvent;
import kr.toxicity.model.api.nms.HitBoxListener;
import kr.toxicity.model.api.util.EventUtil;
import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.function.BonePredicate;
import kr.toxicity.model.api.util.lazy.LazyFloatProvider;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Entity tracker
 */
public class EntityTracker extends Tracker {
    private final EntityTrackerRegistry registry;

    private final AtomicInteger damageTintValue = new AtomicInteger(0xFF8080);
    private final AtomicLong damageTint = new AtomicLong(-1);
    private final Set<UUID> markForSpawn = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final HeadRotationProperty headRotationProperty = new HeadRotationProperty();

    /**
     * Creates entity tracker
     * @param registry registry
     * @param pipeline render instance
     * @param modifier modifier
     * @param preUpdateConsumer task on pre-update
     */
    @ApiStatus.Internal
    public EntityTracker(@NotNull EntityTrackerRegistry registry, @NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        super(pipeline, modifier);
        this.registry = registry;

        var entity = registry.entity();
        var adapter = registry.adapter();
        var scale = FunctionUtil.throttleTickFloat(() -> scaler().scale(this));
        //Shadow
        if (modifier.shadow()) {
            var shadow = BetterModel.plugin().nms().create(entity.getLocation());
            var baseScale = (float) pipeline.bones()
                    .stream()
                    .filter(b -> b.getGroup().getParent().visibility())
                    .map(b -> b.getGroup().getHitBox())
                    .filter(Objects::nonNull)
                    .mapToDouble(b -> Math.max(b.box().x(), b.box().z()))
                    .max()
                    .orElse(0D);
            tick(((t, s) -> {
                shadow.shadowRadius(scale.getAsFloat() * baseScale);
                shadow.sync(adapter);
                shadow.sendEntityData(s.getTickBundler());
                shadow.syncPosition(adapter, s.getTickBundler());
            }));
            pipeline.spawnPacketHandler(shadow::spawn);
            pipeline.despawnPacketHandler(shadow::remove);
            pipeline.hidePacketHandler(b -> shadow.sendEntityData(false, b));
            pipeline.showPacketHandler(shadow::sendEntityData);
        }

        pipeline.hideFilter(p -> !p.canSee(registry.entity()));

        //Animation
        pipeline.defaultPosition(FunctionUtil.throttleTick(() -> adapter.passengerPosition().mul(-1)));
        pipeline.scale(scale);
        Function<Quaternionf, Quaternionf> headRotator = r -> r.mul(MathUtil.toQuaternion(headRotationProperty.get()));
        pipeline.addRotationModifier(
                BonePredicate.of(BonePredicate.State.NOT_SET, r -> r.getName().tagged(BoneTags.HEAD)),
                headRotator
        );
        pipeline.addRotationModifier(
                BonePredicate.of(BonePredicate.State.TRUE, r -> r.getName().tagged(BoneTags.HEAD_WITH_CHILDREN)),
                headRotator
        );

        var damageTickProvider = FunctionUtil.throttleTickFloat(adapter::damageTick);
        var walkSupplier = FunctionUtil.throttleTickBoolean(() -> adapter.onWalk() || damageTickProvider.getAsFloat() > 0.25 || pipeline.bones().stream().anyMatch(e -> {
            var hitBox = e.getHitBox();
            return hitBox != null && hitBox.onWalk();
        }));
        var walkSpeedSupplier = FunctionUtil.throttleTickFloat(modifier.damageAnimation() ? () -> adapter.walkSpeed() + 4F * (float) Math.sqrt(damageTickProvider.getAsFloat()) : () -> 1F);
        pipeline.animate("walk", new AnimationModifier(walkSupplier, 6, 0, AnimationIterator.Type.LOOP, walkSpeedSupplier));
        pipeline.animate("idle_fly", new AnimationModifier(adapter::fly, 6, 0, AnimationIterator.Type.LOOP, 1F));
        pipeline.animate("walk_fly", new AnimationModifier(() -> adapter.fly() && walkSupplier.getAsBoolean(), 6, 0, AnimationIterator.Type.LOOP, walkSpeedSupplier));
        pipeline.animate("spawn", AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
        BetterModel.plugin().scheduler().task(entity, () -> {
            if (isClosed()) return;
            createHitBox();
        });
        tick((t, s) -> updateBaseEntity0());
        tick((t, s) -> {
            var reader = t.pipeline.getScriptProcessor().getCurrentReader();
            if (reader == null) return;
            var script = reader.script();
            if (script == null) return;
            BetterModel.plugin().scheduler().task(entity, () -> script.accept(entity));
        });
        tick((t, s) -> {
            if (damageTint.getAndDecrement() == 0) tint(-1);
        });
        rotation(() -> new ModelRotation(adapter.pitch(), entity instanceof LivingEntity ? adapter.bodyYaw() : adapter.yaw()));
        preUpdateConsumer.accept(this);
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

    @Override
    public @NotNull ModelRotation rotation() {
        return registry.adapter().dead() ? pipeline.getRotation() : super.rotation();
    }

    private void createHitBox(@NotNull Predicate<RenderedBone> predicate) {
        createHitBox(predicate, HitBoxListener.EMPTY);
    }

    /**
     * Syncs this tracker to base entity's data.
     */
    public void updateBaseEntity() {
        BetterModel.plugin().scheduler().asyncTaskLater(1, () -> {
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
        pipeline.createHitBox(registry.adapter(), predicate, listener);
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
        if (get <= 0 && damageTint.compareAndSet(get, 10)) task(() -> tint(damageTintValue()));
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
        return sourceEntity().getLocation();
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
        pipeline.moveDuration(duration);
        forceUpdate(true);
    }

    /**
     * Cancels damage tint task
     */
    public void cancelDamageTint() {
        damageTint.set(-1);
    }

    /**
     * Gets head rotation property
     * @return rotation property
     */
    public @NotNull HeadRotationProperty headRotationProperty() {
        return headRotationProperty;
    }

    /**
     * Refresh this tracker
     */
    @ApiStatus.Internal
    public void refresh() {
        BetterModel.plugin().scheduler().task(registry.entity(), () -> pipeline.createHitBox(registry.adapter(), r -> r.getHitBox() != null, null));
    }

    /**
     * Marks specific player for spawning
     * @return success
     */
    public boolean markPlayerForSpawn(@NotNull OfflinePlayer player) {
        return markForSpawn.add(player.getUniqueId());
    }

    /**
     * Unmarks specific player for spawning
     * @return success
     */
    public boolean unmarkPlayerForSpawn(@NotNull OfflinePlayer player) {
        return markForSpawn.remove(player.getUniqueId());
    }

    /**
     * Checks this model can be spawned at given player
     * @param player target player
     * @return can be spawned
     */
    public boolean canBeSpawnedAt(@NotNull OfflinePlayer player) {
        return markForSpawn.isEmpty() || markForSpawn.contains(player.getUniqueId());
    }

    /**
     * Head rotation property
     */
    public final class HeadRotationProperty implements Supplier<Vector3f> {
        private float rotationDelay = 150;
        private float minRotation = -90;
        private float maxRotation = 90;
        private final Supplier<Vector3f> delegate = LazyFloatProvider.ofVector(TRACKER_TICK_INTERVAL, () -> rotationDelay, () -> new Vector3f(
                Math.clamp(registry.adapter().pitch(), minRotation, maxRotation),
                Math.clamp(-registry.adapter().yaw() + registry.adapter().bodyYaw(), minRotation, maxRotation),
                0
        ));

        /**
         * Private initializer
         */
        private HeadRotationProperty() {
        }

        @Override
        public @NotNull Vector3f get() {
            return delegate.get();
        }

        /**
         * Sets rotation delay
         * @param delay delay
         */
        public synchronized void delay(float delay) {
            rotationDelay = delay;
        }

        /**
         * Sets min rotation degree
         * @param min rotation degree
         */
        public void minRotation(float min) {
            rotation(min, maxRotation);
        }

        /**
         * Sets max rotation degree
         * @param max rotation degree
         */
        public void maxRotation(float max) {
            rotation(minRotation, max);
        }

        /**
         * Sets rotation degree
         * @param min min rotation degree
         * @param max max rotation degree
         */
        public synchronized void rotation(float min, float max) {
            minRotation = Math.min(min, max);
            maxRotation = Math.max(min, max);
        }
    }
}
