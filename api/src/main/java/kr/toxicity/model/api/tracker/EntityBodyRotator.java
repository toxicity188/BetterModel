/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.lazy.LazyFloatProvider;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages the body and head rotation logic for an entity tracker.
 * <p>
 * This class handles the complex interactions between body yaw, head yaw, and pitch,
 * including smoothing, clamping, and player-specific behaviors.
 * </p>
 *
 * @since 1.15.2
 */
public final class EntityBodyRotator {
    private final EntityTrackerRegistry registry;
    private final BaseEntity entity;
    private final LazyFloatProvider provider;
    private final Supplier<Vector3f> headSupplier;
    private final Supplier<ModelRotation> bodySupplier;
    private final AtomicBoolean rotationLock = new AtomicBoolean();
    private int tick;
    private ModelRotation rotation;
    private volatile boolean headUneven;
    private volatile boolean bodyUneven;
    private volatile boolean playerMode;
    private volatile float minBody;
    private volatile float maxBody;
    private volatile float minHead;
    private volatile float maxHead;
    private volatile float stable;
    private volatile int rotationDuration;
    private volatile int rotationDelay;

    static @NotNull RotatorData defaultData() {
        return new RotatorData(
            false,
            false,
            false,
            -75,
            75,
            -75,
            75,
            15,
            10,
            10
        );
    }

    EntityBodyRotator(@NotNull EntityTrackerRegistry registry) {
        this.registry = registry;
        this.entity = registry.entity();
        this.rotation = new ModelRotation(
            entity.pitch(),
            entity.bodyYaw()
        );
        this.provider = new LazyFloatProvider(entity.bodyYaw(), () -> rotationDuration * MathUtil.MINECRAFT_TICK_MILLS);
        headSupplier = LazyFloatProvider.ofVector(Tracker.TRACKER_TICK_INTERVAL, () -> 4 * MathUtil.MINECRAFT_TICK_MILLS, () -> {
            var value = bodyRotation().y() - entity.headYaw();
            if (value > 180) value -= 360;
            else if (value < -180) value += 360;
            return new Vector3f(
                clampHead(entity.pitch()),
                clampHead(value),
                0
            );
        });
        bodySupplier = FunctionUtil.throttleTick(() -> new ModelRotation(
            entity.pitch(),
            bodyRotation0()
        ));
        reset();
    }

    private float clampHead(float value) {
        return Math.clamp(value, headUneven ? minHead : -maxHead, maxHead);
    }
    private float clampBody(float value, float compare) {
        return Math.clamp(value, compare + (bodyUneven ? minBody : -maxBody), compare + maxBody);
    }

    /**
     * Locks or unlocks the rotation updates.
     *
     * @param lock true to lock, false to unlock
     * @return true if the state changed
     * @since 1.15.2
     */
    public boolean lockRotation(boolean lock) {
        return rotationLock.compareAndSet(!lock, lock);
    }

    @NotNull ModelRotation bodyRotation() {
        return rotationLock.get() ? rotation : (rotation = bodySupplier.get());
    }

    private float bodyRotation0() {
        if (playerMode) return entity.headYaw();
        if (registry.hasControllingPassenger()) return entity.bodyYaw();
        var headYaw = entity.headYaw();
        if (MathUtil.isSimilar(headYaw, rotation.y(), MathUtil.DEGREES_TO_PACKED_BYTE)) tick = 0;
        if (entity.onWalk()) {
            tick = 0;
            return stableBodyYaw();
        } else if (++tick > rotationDelay) {
            var providedYaw = provider.updateAndGet(headYaw);
            return clampBody(providedYaw, headYaw);
        }
        provider.storedValue(rotation.y());
        return rotation.y();
    }

    private float stableBodyYaw() {
        var yaw = entity.bodyYaw();
        var headYaw = entity.headYaw();
        var minStable = correctYaw(headYaw - stable);
        var maxStable = correctYaw(headYaw + stable);
        return Math.clamp(yaw, Math.min(minStable, maxStable), Math.max(minStable, maxStable));
    }

    private static float correctYaw(float target) {
        if (target < 0) target += 360;
        return target % 360;
    }

    @NotNull Vector3f headRotation() {
        return headSupplier.get();
    }

    /**
     * Configures the rotator using a consumer.
     *
     * @param consumer the configuration consumer
     * @since 1.15.2
     */
    public void setValue(@NotNull Consumer<RotatorData> consumer) {
        Objects.requireNonNull(consumer);
        var data = createData();
        consumer.accept(data);
        setValue(data);
    }

    synchronized void setValue(@NotNull RotatorData data) {
        data.set(this);
    }

    /**
     * Resets the rotator to default settings.
     *
     * @since 1.15.2
     */
    public void reset() {
        setValue(defaultData());
    }

    synchronized @NotNull RotatorData createData() {
        return new RotatorData(
            headUneven,
            bodyUneven,
            playerMode,
            minBody,
            maxBody,
            minHead,
            maxHead,
            stable,
            rotationDuration,
            rotationDelay
        );
    }

    /**
     * Configuration data for the entity body rotator.
     *
     * @since 1.15.2
     */
    @Setter
    @AllArgsConstructor
    public static final class RotatorData {

        @SerializedName("head_uneven")
        private boolean headUneven;
        @SerializedName("body_uneven")
        private boolean bodyUneven;
        @SerializedName("player_mode")
        private boolean playerMode;
        @SerializedName("min_body")
        private float minBody;
        @SerializedName("max_body")
        private float maxBody;
        @SerializedName("min_head")
        private float minHead;
        @SerializedName("max_head")
        private float maxHead;
        @SerializedName("stable")
        private float stable;
        @SerializedName("rotation_duration")
        private int rotationDuration;
        @SerializedName("rotation_delay")
        private int rotationDelay;

        private void set(@NotNull EntityBodyRotator rotator) {
            rotator.headUneven = headUneven;
            rotator.bodyUneven = bodyUneven;
            rotator.playerMode = playerMode;
            rotator.minBody = Math.min(minBody, maxBody);
            rotator.maxBody = Math.max(minBody, maxBody);
            rotator.minHead = Math.min(minHead, maxHead);
            rotator.maxHead = Math.max(minHead, maxHead);
            rotator.stable = Math.max(stable, 0);
            rotator.rotationDuration = Math.max(rotationDuration, 0);
            rotator.rotationDelay = Math.max(rotationDelay, 0);
        }
    }
}
