package kr.toxicity.model.api.util.lazy;

import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.function.FloatSupplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lazy float provider
 */
@ApiStatus.Internal
public final class LazyFloatProvider {
    private final FloatSupplier requiredTime;
    private long time = System.currentTimeMillis();
    private float storedValue;
    private boolean first = true;

    /**
     * Creates from time
     * @param requiredTime required time
     */
    public LazyFloatProvider(long requiredTime) {
        this((float) requiredTime);
    }
    /**
     * Creates from time
     * @param requiredTime required time
     */
    public LazyFloatProvider(float requiredTime) {
        this(() -> requiredTime);
    }
    /**
     * Creates from time supplier
     * @param requiredTime required time supplier
     */
    public LazyFloatProvider(@NotNull FloatSupplier requiredTime) {
        this.requiredTime = requiredTime;
    }

    /**
     * Creates from time supplier
     * @param requiredTime required time supplier
     * @param initialValue initial value
     */
    public LazyFloatProvider(float initialValue, @NotNull FloatSupplier requiredTime) {
        this(requiredTime);
        this.storedValue = initialValue;
    }

    /**
     * Updates and gets float
     * @param updateValue destination value
     * @return interpolated value
     */
    public float updateAndGet(float updateValue) {
        var req = requiredTime.getAsFloat();
        if (req <= 0 || first) {
            first = false;
            return storedValue = updateValue;
        }
        var current = System.currentTimeMillis();
        var alpha = Math.clamp((float) (current - time) / req, 0, 1);
        time = current;
        return storedValue = InterpolationUtil.lerp(
                storedValue,
                updateValue,
                alpha
        );
    }

    /**
     * Sets stored value
     * @param storedValue new value
     */
    public void storedValue(float storedValue) {
        this.storedValue = storedValue;
        time = System.currentTimeMillis();
    }

    /**
     * Gets lazy provider of vector
     * @param tick throttle tick
     * @param requiredTime required time
     * @param delegate source provider
     * @return lazy provider
     */
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
