package kr.toxicity.model.api.script;

import com.google.common.util.concurrent.AtomicDouble;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.data.raw.Datapoint;
import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.util.InterpolationUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A script data of blueprint.
 * @param name script name
 * @param length playtime
 * @param scripts scripts
 */
@ApiStatus.Internal
public record BlueprintScript(@NotNull String name, @NotNull AnimationIterator.Type type, float length, @NotNull List<TimeScript> scripts) {

    /**
     * Creates animation's script
     * @param animation animation
     * @param animator animator
     * @return blueprint script
     */
    public static @NotNull BlueprintScript from(@NotNull ModelAnimation animation, @NotNull ModelAnimator animator) {
        var stream = animator.keyframes()
                .stream()
                .map(d -> AnimationScript.of(d.dataPoints()
                        .stream()
                        .map(Datapoint::script)
                        .filter(Objects::nonNull)
                        .map(raw -> BetterModel.plugin().scriptManager().build(raw))
                        .filter(Objects::nonNull)
                        .toList()
                ).time(d.time()));
        if (animator.keyframes().getFirst().time() > 0) {
            stream = Stream.concat(Stream.of(TimeScript.EMPTY), stream);
        }
        var time = animation.length();
        if (time > 0) {
            stream = Stream.concat(stream, Stream.of(AnimationScript.EMPTY.time(time)));
        }
        var doubleCache = new AtomicDouble();
        return new BlueprintScript(
                animation.name(),
                animation.loop(),
                animation.length(),
                stream.distinct()
                        .map(t -> t.time(InterpolationUtil.roundTime(t.time() - (float) doubleCache.getAndSet(t.time()))))
                        .toList()
        );
    }

    public static @NotNull BlueprintScript fromEmpty(@NotNull ModelAnimation animation) {
        return new BlueprintScript(
                animation.name(),
                animation.loop(),
                animation.length(),
                List.of(TimeScript.EMPTY, AnimationScript.EMPTY.time(animation.length()))
        );
    }

    public @NotNull AnimationIterator<TimeScript> iterator() {
        return type.create(scripts);
    }
}
