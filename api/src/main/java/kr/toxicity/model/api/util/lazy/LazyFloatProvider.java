package kr.toxicity.model.api.util.lazy;

import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.VectorUtil;
import kr.toxicity.model.api.util.function.FloatSupplier;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.function.Supplier;

public final class LazyFloatProvider {
    private final FloatSupplier requiredTime;
    private long time = System.currentTimeMillis();
    private float storedValue;
    private boolean first = true;

    public LazyFloatProvider(long requiredTime) {
        this((float) requiredTime);
    }
    public LazyFloatProvider(float requiredTime) {
        this(() -> requiredTime);
    }
    public LazyFloatProvider(@NotNull FloatSupplier requiredTime) {
        this.requiredTime = requiredTime;
    }

    public float updateAndGet(float updateValue) {
        var req = requiredTime.getAsFloat();
        if (req <= 0 || first) {
            first = false;
            return storedValue = updateValue;
        }
        var current = System.currentTimeMillis();
        var alpha = Math.clamp((float) (current - time) / req, 0, 1);
        time = current;
        return storedValue = VectorUtil.linear(
                storedValue,
                updateValue,
                alpha
        );
    }

    public static @NotNull Supplier<Vector3f> ofVector(long tick, @NotNull FloatSupplier requiredTime, @NotNull Supplier<Vector3f> delegate) {
        Objects.requireNonNull(requiredTime);
        Objects.requireNonNull(delegate);
        var xLazy = new LazyFloatProvider(requiredTime);
        var yLazy = new LazyFloatProvider(requiredTime);
        var zLazy = new LazyFloatProvider(requiredTime);
        return FunctionUtil.throttleTick(tick, () -> {
            var get = delegate.get();
            get.x = xLazy.updateAndGet(get.x);
            get.y = yLazy.updateAndGet(get.y);
            get.z = zLazy.updateAndGet(get.z);
            return get;
        });
    }
}
