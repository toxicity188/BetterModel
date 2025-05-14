package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.AnimationPoint;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import kr.toxicity.model.api.script.BlueprintScript;
import kr.toxicity.model.api.util.VectorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Collectors;

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
            var frameList = new ArrayList<>(entry.getValue().keyframes());
            frameList.sort(Comparator.naturalOrder());
            for (ModelKeyframe keyframe : frameList) {
                builder.addFrame(keyframe);
            }
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
                newMap.isEmpty() ? List.of(new AnimationMovement(0, null, null, null)) : newMap.values()
                        .iterator()
                        .next()
                        .keyFrame()
                        .stream()
                        .map(a -> new AnimationMovement(a.time(), null, null, null))
                        .toList()
        );
    }

    private static @NotNull Map<BoneName, BlueprintAnimator> newMap(@NotNull Map<BoneName, BlueprintAnimator.AnimatorData> oldMap) {
        var newMap = new HashMap<BoneName, BlueprintAnimator>();
        Set<Float> floatSet = new TreeSet<>(Comparator.naturalOrder());
        oldMap.values().forEach(a -> a.points().stream().map(t -> t.position().time()).forEach(floatSet::add));
        for (Map.Entry<BoneName, BlueprintAnimator.AnimatorData> entry : oldMap.entrySet()) {
            var list = getAnimationMovements(floatSet, entry);
            newMap.put(entry.getKey(), new BlueprintAnimator(
                    entry.getValue().name(),
                    list
            ));
        }
        return newMap;
    }

    private static @NotNull List<AnimationMovement> getAnimationMovements(@NotNull Set<Float> floatSet, @NotNull Map.Entry<BoneName, BlueprintAnimator.AnimatorData> entry) {
        var frame = entry.getValue().points();
        if (frame.isEmpty()) return Collections.emptyList();
        var list = VectorUtil.putAnimationPoint(frame, floatSet).stream().map(AnimationPoint::toMovement).collect(Collectors.toList());
        return processFrame(list);
    }

    private static @NotNull List<AnimationMovement> processFrame(@NotNull List<AnimationMovement> target) {
        if (target.size() <= 1) return target;
        var list = new ArrayList<AnimationMovement>();
        list.add(target.getFirst());
        for (int i = 1; i < target.size(); i++) {
            var get = target.get(i);
            list.add(get.time(get.time() - target.get(i - 1).time()));
        }
        return list;
    }

    /**
     * Gets iterator.
     * @param type type
     * @return iterator
     */
    public @NotNull AnimationIterator emptyIterator(@NotNull AnimationIterator.Type type) {
        return type.create(emptyAnimator);
    }
}
