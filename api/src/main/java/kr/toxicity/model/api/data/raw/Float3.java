package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Function;

/**
 * A three float value (origin, rotation)
 * @param x x
 * @param y y
 * @param z z
 */
@ApiStatus.Internal
public record Float3(
        float x,
        float y,
        float z
) {

    /**
     * Creates floats
     * @param value scala
     */
    public Float3(float value) {
        this(value, value, value);
    }
    /**
     * Center
     */
    public static final Float3 CENTER = new Float3(8, 8, 8);
    /**
     * Zero
     */
    public static final Float3 ZERO = new Float3(0, 0, 0);

    /**
     * Parser
     */
    public static final Function<JsonElement, Float3> PARSER = element -> {
        if (element == null) return new Float3(0, 0, 0);
        var array = element.getAsJsonArray();
        return new Float3(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat()
        );
    };

    /**
     * Adds other floats.
     * @param other other floats
     * @return new floats
     */
    public @NotNull Float3 plus(@NotNull Float3 other) {
        return new Float3(
                x + other.x,
                y + other.y,
                z + other.z
        );
    }

    /**
     * Converts zxy euler to xyz euler (Minecraft)
     * @return new float
     */
    public @NotNull Float3 convertToMinecraftDegree() {
        var vec = MathUtil.toMinecraftVector(toVector());
        return new Float3(vec.x, vec.y, vec.z);
    }

    /**
     * Rotates this float
     * @param quaternionf rotation
     * @return new float
     */
    public @NotNull Float3 rotate(@NotNull Quaternionf quaternionf) {
        var vec = toVector().rotate(quaternionf);
        return new Float3(vec.x, vec.y, vec.z);
    }


    /**
     * Subtracts other floats.
     * @param other other floats
     * @return new floats
     */
    public @NotNull Float3 minus(@NotNull Float3 other) {
        return new Float3(
                x - other.x,
                y - other.y,
                z - other.z
        );
    }

    /**
     * Multiplies floats.
     * @param value multiplier
     * @return new floats
     */
    public @NotNull Float3 times(float value) {
        return new Float3(
                x * value,
                y * value,
                z * value
        );
    }
    /**
     * Divides floats.
     * @param value multiplier
     * @return new floats
     */
    public @NotNull Float3 div(float value) {
        return new Float3(
                x / value,
                y / value,
                z / value
        );
    }

    /**
     * Converts floats to json array.
     * @return json array
     */
    public @NotNull JsonArray toJson() {
        var array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }

    /**
     * Converts floats to vector.
     * @return vector
     */
    public @NotNull Vector3f toVector() {
        return new Vector3f(x, y, z);
    }
}
