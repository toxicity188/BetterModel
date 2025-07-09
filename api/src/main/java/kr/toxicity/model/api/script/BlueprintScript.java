package kr.toxicity.model.api.script;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.data.raw.Datapoint;
import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
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
        var stream = processFrame(animator.keyframes())
                .stream()
                .map(d -> AnimationScript.of(d.dataPoints()
                        .stream()
                        .map(Datapoint::script)
                        .filter(Objects::nonNull)
                        .map(raw -> BetterModel.plugin().scriptManager().build(raw))
                        .filter(Objects::nonNull)
                        .toList()
                ).time(d.time()));
        return new BlueprintScript(
                animation.name(),
                animation.loop(),
                animation.length(),
                Stream.concat(stream, Stream.of(AnimationScript.EMPTY.time(animation.length() - animator.keyframes().getLast().time()))).toList());
    }

    public @NotNull AnimationIterator<TimeScript> iterator() {
        return type.create(scripts);
    }

    private static @NotNull List<ModelKeyframe> processFrame(@NotNull List<ModelKeyframe> target) {
        if (target.size() <= 1) return target;
        return IntStream.range(0, target.size()).mapToObj(i -> {
            if (i == 0) return target.getFirst();
            else {
                var get = target.get(i);
                return get.time(get.time() - target.get(i - 1).time());
            }
        }).toList();
    }
}
