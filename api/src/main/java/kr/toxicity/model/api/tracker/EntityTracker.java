/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.BoneTags;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.event.CreateEntityTrackerEvent;
import kr.toxicity.model.api.event.DismountModelEvent;
import kr.toxicity.model.api.event.MountModelEvent;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.HitBoxListener;
import kr.toxicity.model.api.util.EventUtil;
import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.function.BonePredicate;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Entity tracker
 */
public class EntityTracker extends Tracker {

    private static final BonePredicate CREATE_HITBOX_PREDICATE = BonePredicate.from(b -> b.name().name().equals("hitbox")
            || b.name().tagged(BoneTags.HITBOX)
            || b.getGroup().getMountController().canMount());
    private static final BonePredicate CREATE_NAMETAG_PREDICATE = BonePredicate.from(b -> b.name().tagged(BoneTags.TAG, BoneTags.MOB_TAG, BoneTags.PLAYER_TAG));
    private static final BonePredicate HITBOX_REFRESH_PREDICATE = BonePredicate.from(r -> r.getHitBox() != null);

    private final EntityTrackerRegistry registry;

    private final AtomicInteger damageTintValue = new AtomicInteger(0xFF8080);
    private final AtomicLong damageTint = new AtomicLong(-1);
    private final Set<UUID> markForSpawn = ConcurrentHashMap.newKeySet();

    private final EntityBodyRotator bodyRotator;
    private EntityHideOption hideOption = EntityHideOption.DEFAULT;

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
        bodyRotator = new EntityBodyRotator(registry);

        var entity = registry.entity();
        var adapter = registry.adapter();
        var scale = FunctionUtil.throttleTickFloat(() -> scaler().scale(this));
        //Shadow
        Optional.ofNullable(bone("shadow"))
                .ifPresent(bone -> {
                    var box = bone.getGroup().getHitBox();
                    if (box == null) return;
                    var shadow = BetterModel.plugin().nms().create(entity.getLocation(), d -> d.moveDuration(1));
                    var baseScale = (float) (box.box().x() + box.box().z()) / 4F;
                    tick(((t, s) -> {
                        var wPos = bone.hitBoxPosition();
                        shadow.shadowRadius(scale.getAsFloat() * baseScale);
                        shadow.syncEntity(adapter);
                        shadow.syncPosition(location().add(wPos.x, wPos.y, wPos.z));
                        shadow.sendDirtyEntityData(s.getDataBundler());
                        shadow.sendPosition(adapter, s.getTickBundler());
                    }));
                    pipeline.spawnPacketHandler(shadow::spawnWithEntityData);
                    pipeline.showPacketHandler(shadow::spawnWithEntityData);
                    pipeline.despawnPacketHandler(shadow::remove);
                    pipeline.hidePacketHandler(shadow::remove);
                });

        //Animation
        pipeline.defaultPosition(FunctionUtil.throttleTick(() -> adapter.passengerPosition().mul(-1)));
        pipeline.scale(scale);
        Function<Quaternionf, Quaternionf> headRotator = r -> r.mul(MathUtil.toQuaternion(bodyRotator.headRotation()));
        pipeline.addRotationModifier(
                BonePredicate.of(BonePredicate.State.NOT_SET, r -> r.name().tagged(BoneTags.HEAD)),
                headRotator
        );
        pipeline.addRotationModifier(
                BonePredicate.of(BonePredicate.State.TRUE, r -> r.name().tagged(BoneTags.HEAD_WITH_CHILDREN)),
                headRotator
        );

        var damageTickProvider = FunctionUtil.throttleTickFloat(adapter::damageTick);
        var walkSupplier = FunctionUtil.throttleTickBoolean(() -> adapter.onWalk() || damageTickProvider.getAsFloat() > 0.25 || pipeline.bones().stream().anyMatch(e -> {
            var hitBox = e.getHitBox();
            return hitBox != null && hitBox.onWalk();
        }));
        var walkSpeedSupplier = modifier.damageAnimation() ? FunctionUtil.throttleTickFloat(() -> adapter.walkSpeed() + (float) Math.sqrt(damageTickProvider.getAsFloat())) : null;
        animate("walk", new AnimationModifier(walkSupplier, 6, 0, AnimationIterator.Type.LOOP, walkSpeedSupplier));
        animate("idle_fly", new AnimationModifier(adapter::fly, 6, 0, AnimationIterator.Type.LOOP, null));
        animate("walk_fly", new AnimationModifier(() -> adapter.fly() && walkSupplier.getAsBoolean(), 6, 0, AnimationIterator.Type.LOOP, walkSpeedSupplier));
        animate("spawn", AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
        createNametag(CREATE_NAMETAG_PREDICATE, (bone, tag) -> {
            if (bone.name().tagged(BoneTags.PLAYER_TAG)) {
                tag.alwaysVisible(true);
            } else if (bone.name().tagged(BoneTags.MOB_TAG)) {
                tag.alwaysVisible(false);
            } else tag.alwaysVisible(entity instanceof Player);
            tag.component(adapter.customName());
        });
        pipeline.eventDispatcher().handleCreateHitBox((b, l) -> l.mount((h, e) -> {
                    registry.mountedHitBoxCache.put(e.getUniqueId(), new EntityTrackerRegistry.MountedHitBox(b, e, h));
                    EventUtil.call(new MountModelEvent(this, b, h, e));
                })
                .dismount((h, e) -> {
                    registry.mountedHitBoxCache.remove(e.getUniqueId());
                    EventUtil.call(new DismountModelEvent(this, b, h, e));
                }));
        BetterModel.plugin().scheduler().task(entity, () -> {
            if (isClosed()) return;
            createHitBox(null, CREATE_HITBOX_PREDICATE);
        });
        tick((t, s) -> updateBaseEntity0());
        tick((t, s) -> {
            if (damageTint.getAndDecrement() == 0) update(TrackerUpdateAction.previousTint());
        });
        rotation(bodyRotator::bodyRotation);
        preUpdateConsumer.accept(this);
        EventUtil.call(new CreateEntityTrackerEvent(this));
    }

    @Override
    public @NotNull ModelRotation rotation() {
        return registry.adapter().dead() ? pipeline.getRotation() : super.rotation();
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
        var loc = location();
        displays().forEach(d -> {
            d.syncEntity(registry.adapter());
            d.syncPosition(loc);
        });
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
     * @param listener listener
     * @param predicate predicate
     * @return success
     */
    public boolean createHitBox(@Nullable HitBoxListener listener, @NotNull BonePredicate predicate) {
        return createHitBox(registry.adapter(), listener, predicate);
    }

    /**
     * Gets or creates model's hitbox
     * @param listener listener
     * @param predicate predicate
     * @return hitbox or null
     */
    public @Nullable HitBox hitbox(@Nullable HitBoxListener listener, @NotNull Predicate<RenderedBone> predicate) {
        return hitbox(registry.adapter(), listener, predicate);
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
        if (get < 0 && damageTint.compareAndSet(get, 10)) task(() -> update(TrackerUpdateAction.tint(damageTintValue())));
    }

    @Override
    public void despawn() {
        if (registry.adapter().dead()) {
            close(CloseReason.DESPAWN);
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
     * Cancels damage tint task
     */
    public void cancelDamageTint() {
        damageTint.set(-1);
    }

    /**
     * Refresh this tracker
     */
    @ApiStatus.Internal
    public void refresh() {
        updateBaseEntity0();
        BetterModel.plugin().scheduler().task(registry.entity(), () -> createHitBox(null, HITBOX_REFRESH_PREDICATE));
    }

    /**
     * Marks specific player for spawning
     * @param player player
     * @return success
     */
    public boolean markPlayerForSpawn(@NotNull OfflinePlayer player) {
        return markForSpawn.add(player.getUniqueId());
    }

    /**
     * Marks specific player for spawning
     * @param uuids uuids
     * @return success
     */
    public boolean markPlayerForSpawn(@NotNull Set<UUID> uuids) {
        return markForSpawn.addAll(uuids);
    }

    /**
     * Unmarks specific player for spawning
     * @param player player
     * @return success
     */
    public boolean unmarkPlayerForSpawn(@NotNull OfflinePlayer player) {
        return markForSpawn.remove(player.getUniqueId());
    }

    /**
     * Creates tracker data
     * @return tracker data
     */
    public @NotNull TrackerData asTrackerData() {
        return new TrackerData(
                name(),
                scaler,
                rotator,
                modifier,
                bodyRotator.createData(),
                hideOption,
                markForSpawn
        );
    }

    /**
     * Gets body rotator
     * @return body rotator
     */
    public @NotNull EntityBodyRotator bodyRotator() {
        return bodyRotator;
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
     * Gets hide option of this tracker
     * @return hide option
     */
    public @NotNull EntityHideOption hideOption() {
        return hideOption;
    }

    /**
     * Sets hide option of this tracker
     * @param hideOption hide option
     */
    public void hideOption(@NotNull EntityHideOption hideOption) {
        this.hideOption = Objects.requireNonNull(hideOption);
    }

    /**
     * Checks this tracker's data can be saved
     * @return can be saved
     */
    public boolean canBeSaved() {
        return pipeline.getParent().type().isCanBeSaved();
    }
}
