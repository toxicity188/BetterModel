package kr.toxicity.model.api.tracker;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.nms.EntityAdapter;
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

public final class EntityBodyRotator {
    private final EntityAdapter adapter;
    private final LazyFloatProvider provider;
    private final Supplier<Vector3f> headSupplier;
    private final Supplier<ModelRotation> bodySupplier;
    private final AtomicBoolean rotationLock = new AtomicBoolean();
    private int tick;
    private ModelRotation rotation;
    private Vector3f lastHeadRotation = new Vector3f();
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

    EntityBodyRotator(@NotNull EntityAdapter adapter) {
        this.adapter = adapter;
        this.rotation = new ModelRotation(
                adapter.pitch(),
                adapter.bodyYaw()
        );
        this.provider = new LazyFloatProvider(adapter.bodyYaw(), () -> rotationDuration * MathUtil.MINECRAFT_TICK_MILLS);
        headSupplier = LazyFloatProvider.ofVector(Tracker.TRACKER_TICK_INTERVAL, () -> 4 * MathUtil.MINECRAFT_TICK_MILLS, () -> {
            var value = rotation.y() - adapter.headYaw();
            if (value > 180) value -= 360;
            else if (value < -180) value += 360;
            return new Vector3f(
                    clampHead(adapter.pitch()),
                    clampHead(value),
                    0
            );
        });
        bodySupplier = FunctionUtil.throttleTick(() -> new ModelRotation(
                adapter.pitch(),
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

    public boolean lockRotation(boolean lock) {
        return rotationLock.compareAndSet(!lock, lock);
    }

    @NotNull ModelRotation bodyRotation() {
        return rotationLock.get() ? rotation : (rotation = bodySupplier.get());
    }

    private float bodyRotation0() {
        if (playerMode) return adapter.headYaw();
        var headYaw = adapter.headYaw();
        if (isSimilar(headYaw, rotation.y())) tick = 0;
        if (adapter.onWalk()) {
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
        var yaw = adapter.bodyYaw();
        var headYaw = adapter.headYaw();
        var minStable = correctYaw(headYaw - stable);
        var maxStable = correctYaw(headYaw + stable);
        return Math.clamp(yaw, Math.min(minStable, maxStable), Math.max(minStable, maxStable));
    }

    private static float correctYaw(float target) {
        if (target < 0) target += 360;
        return target % 360;
    }

    private static boolean isSimilar(float a, float b) {
        return Math.abs(a - b) < MathUtil.FRAME_EPSILON;
    }

    @NotNull Vector3f headRotation() {
        return rotationLock.get() ? lastHeadRotation : (lastHeadRotation = headSupplier.get());
    }

    public void setValue(@NotNull Consumer<RotatorData> consumer) {
        Objects.requireNonNull(consumer);
        var data = createData();
        consumer.accept(data);
        synchronized (this) {
            data.set(this);
        }
    }

    void setValue(@NotNull RotatorData data) {
        synchronized (this) {
            data.set(this);
        }
    }

    public void reset() {
        setValue(RotatorData.defaultData());
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

        private void set(@NotNull EntityBodyRotator rotator) {
            rotator.headUneven = headUneven;
            rotator.bodyUneven = bodyUneven;
            rotator.playerMode = playerMode;
            rotator.minBody = Math.min(minBody, maxBody);
            rotator.maxBody = Math.max(minBody, maxBody);
            rotator.minHead = Math.min(minHead, maxHead);
            rotator.maxHead = Math.max(minBody, maxHead);
            rotator.stable = Math.max(stable, 0);
            rotator.rotationDuration = Math.max(rotationDuration, 0);
            rotator.rotationDelay = Math.max(rotationDelay, 0);
        }
    }
}
