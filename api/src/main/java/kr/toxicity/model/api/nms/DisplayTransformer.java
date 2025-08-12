package kr.toxicity.model.api.nms;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Display transformer
 */
public interface DisplayTransformer {

    /**
     * Transforms this display
     * @param duration duration
     * @param position position
     * @param scale scale
     * @param rotation rotation
     * @param bundler bundler
     */
    void transform(int duration, @NotNull Vector3f position, @NotNull Vector3f scale, @NotNull Quaternionf rotation, @NotNull PacketBundler bundler);

    /**
     * Sends transformation
     * @param bundler packet bundler
     */
    void sendTransformation(@NotNull PacketBundler bundler);
}
