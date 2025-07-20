package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.nms.EntityAdapter;
import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.lazy.LazyFloatProvider;
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
    private volatile float minBody = -75F;
    private volatile float maxBody = 75F;
    private volatile float minHead = -75F;
    private volatile float maxHead = 75F;
    private volatile float stable = 15F;
    private volatile int rotationDuration = 10;
    private volatile int rotationDelay = 10;

    EntityBodyRotator(@NotNull EntityAdapter adapter) {
        this.adapter = adapter;
        this.rotation = new ModelRotation(
                adapter.pitch(),
                adapter.bodyYaw()
        );
        this.provider = new LazyFloatProvider(adapter.bodyYaw(), () -> rotationDuration * MathUtil.MINECRAFT_TICK_MILLS);
        headSupplier = LazyFloatProvider.ofVector(MathUtil.MINECRAFT_TICK_MILLS, () -> 4 * MathUtil.MINECRAFT_TICK_MILLS, () -> {
            var value = rotation.y() - adapter.headYaw();
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

    public @NotNull ModelRotation bodyRotation() {
        return rotationLock.get() ? rotation : (rotation = bodySupplier.get());
    }

    private float bodyRotation0() {
        var headYaw = adapter.headYaw();
        if (isSimilar(headYaw, rotation.y())) tick = 0;
        if (adapter.onWalk()) {
            tick = 0;
            return stableBodyYaw();
        } else if (tick++ > rotationDelay) {
            var providedYaw = provider.updateAndGet(headYaw);
            return clampBody(providedYaw, headYaw);
        }
        provider.storedValue(rotation.y());
        return rotation.y();
    }

    private float stableBodyYaw() {
        if (playerMode) return adapter.headYaw();
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
        return Math.abs(a - b) < 256F / 360F;
    }

    public @NotNull Vector3f headRotation() {
        return rotationLock.get() ? lastHeadRotation : (lastHeadRotation = headSupplier.get());
    }

    public void setValue(@NotNull Consumer<Setter> consumer) {
        Objects.requireNonNull(consumer);
        var setter = new Setter();
        consumer.accept(setter);
        synchronized (this) {
            setter.set();
        }
    }

    @lombok.Setter
    public class Setter {

        private boolean headUneven = EntityBodyRotator.this.headUneven;
        private boolean bodyUneven = EntityBodyRotator.this.bodyUneven;
        private boolean playerMode = EntityBodyRotator.this.playerMode;
        private float minBody = EntityBodyRotator.this.minBody;
        private float maxBody = EntityBodyRotator.this.maxBody;
        private float minHead = EntityBodyRotator.this.minHead;
        private float maxHead = EntityBodyRotator.this.maxHead;
        private float stable = EntityBodyRotator.this.stable;
        private int rotationDuration = EntityBodyRotator.this.rotationDuration;
        private int rotationDelay = EntityBodyRotator.this.rotationDelay;

        private Setter() {
        }

        private void set() {
            EntityBodyRotator.this.headUneven = headUneven;
            EntityBodyRotator.this.bodyUneven = bodyUneven;
            EntityBodyRotator.this.playerMode = playerMode;
            EntityBodyRotator.this.minBody = Math.min(minBody, maxBody);
            EntityBodyRotator.this.maxBody = Math.max(minBody, maxBody);
            EntityBodyRotator.this.minHead = Math.min(minHead, maxHead);
            EntityBodyRotator.this.maxHead = Math.max(minBody, maxHead);
            EntityBodyRotator.this.stable = Math.max(stable, 0);
            EntityBodyRotator.this.rotationDuration = Math.max(rotationDuration, 0);
            EntityBodyRotator.this.rotationDelay = Math.max(rotationDelay, 0);
        }
    }
}
