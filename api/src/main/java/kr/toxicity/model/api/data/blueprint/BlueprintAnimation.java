package kr.toxicity.model.api.data.blueprint;

import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet;
import it.unimi.dsi.fastutil.floats.FloatSet;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.AnimationPoint;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.script.BlueprintScript;
import kr.toxicity.model.api.util.VectorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.IntStream;

import static kr.toxicity.model.api.util.CollectionUtil.mapFloat;
import static kr.toxicity.model.api.util.CollectionUtil.mapValue;

/**
 * A model animation.
 * @param name animation name
 * @param loop loop mode
 * @param length frame length
 * @param override override
 * @param animator group animator
 * @param emptyAnimator empty animation ([0, 0, 0]).
 * @param script script
 */
public record BlueprintAnimation(
        @NotNull String name,
        @NotNull AnimationIterator.Type loop,
        float length,
        boolean override,
        @NotNull @Unmodifiable Map<BoneName, BlueprintAnimator> animator,
        @NotNull BlueprintScript script,
        @NotNull List<AnimationMovement> emptyAnimator
) {
    /**
     * Converts from raw animation.
     * @param animation raw animation
     * @return converted animation
     */
    public static @NotNull BlueprintAnimation from(@NotNull ModelAnimation animation) {
        var map = new HashMap<BoneName, BlueprintAnimator.AnimatorData>();
        BlueprintScript blueprintScript = BlueprintScript.emptyOf(animation);
        var animator = animation.animators();
        for (Map.Entry<String, ModelAnimator> entry : animator.entrySet()) {
            var name = entry.getValue().name();
            if (name == null) continue;
            var builder = new BlueprintAnimator.Builder(animation.length());
            entry.getValue().keyframes()
                    .stream()
                    .sorted(Comparator.naturalOrder())
                    .forEach(builder::addFrame);
            if (entry.getKey().equals("effects")) {
                blueprintScript = BlueprintScript.from(animation, entry.getValue());
            }
            else map.put(BoneTagRegistry.parse(name), builder.build(name));
        }
        var newMap = newMap(map);
        return new BlueprintAnimation(
                animation.name(),
                animation.loop(),
                animation.length(),
                animation.override(),
                newMap,
                blueprintScript,
                newMap.isEmpty() ? List.of(new AnimationMovement(0)) : newMap.values()
                        .iterator()
                        .next()
                        .keyFrame()
                        .stream()
                        .map(a -> new AnimationMovement(a.time()))
                        .toList()
        );
    }

    private static @NotNull Map<BoneName, BlueprintAnimator> newMap(@NotNull Map<BoneName, BlueprintAnimator.AnimatorData> oldMap) {
        var floatSet = mapFloat(oldMap.values()
                .stream()
                .flatMap(a -> a.points().stream()), p -> p.position().time(), FloatAVLTreeSet::new);
        return mapValue(oldMap, value -> new BlueprintAnimator(
                value.name(),
                getAnimationMovements(floatSet, value)
        ));
    }

    private static @NotNull List<AnimationMovement> getAnimationMovements(@NotNull FloatSet floatSet, @NotNull BlueprintAnimator.AnimatorData value) {
        var frame = value.points();
        if (frame.isEmpty()) return Collections.emptyList();
        var list = VectorUtil.putAnimationPoint(frame, floatSet).stream().map(AnimationPoint::toMovement).toList();
        return processFrame(list);
    }

    private static @NotNull List<AnimationMovement> processFrame(@NotNull List<AnimationMovement> target) {
        if (target.size() <= 1) return target;
        return IntStream.range(0, target.size()).mapToObj(i -> {
            if (i == 0) return target.getFirst();
            else {
                var get = target.get(i);
                return get.time(get.time() - target.get(i - 1).time());
            }
        }).toList();
    }

    /**
     * Gets iterator.
     * @param type type
     * @return iterator
     */
    public @NotNull AnimationIterator<AnimationMovement> emptyIterator(@NotNull AnimationIterator.Type type) {
        return type.create(emptyAnimator);
    }
}
