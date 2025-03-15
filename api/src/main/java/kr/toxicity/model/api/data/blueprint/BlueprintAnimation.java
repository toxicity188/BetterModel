package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import kr.toxicity.model.api.script.BlueprintScript;
import kr.toxicity.model.api.util.AnimationPoint;
import kr.toxicity.model.api.util.VectorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A model animation.
 * @param name animation name
 * @param length frame length
 * @param animator group animator
 * @param emptyAnimator empty animation ([0, 0, 0]).
 */
public record BlueprintAnimation(
        @NotNull String name,
        float length,
        @NotNull @Unmodifiable Map<String, BlueprintAnimator> animator,
        @NotNull BlueprintScript script,
        @NotNull List<AnimationMovement> emptyAnimator
) {
    /**
     * Converts from raw animation.
     * @param animation raw animation
     * @return converted animation
     */
    public static @NotNull BlueprintAnimation from(@NotNull ModelAnimation animation) {
        var map = new HashMap<String, BlueprintAnimator.AnimatorData>();
        BlueprintScript blueprintScript = BlueprintScript.emptyOf(animation);
        var animator = animation.animators();
        if (animator != null) for (Map.Entry<String, ModelAnimator> entry : animator.entrySet()) {
            var builder = new BlueprintAnimator.Builder(animation.length());
            var frameList = new ArrayList<>(entry.getValue().keyframes());
            frameList.sort(Comparator.naturalOrder());
            for (ModelKeyframe keyframe : frameList) {
                builder.addFrame(keyframe);
            }
            var name = entry.getValue().name();
            if (entry.getKey().equals("effects")) {
                blueprintScript = BlueprintScript.from(animation, entry.getValue());
            }
            else map.put(name, builder.build(name));
        }
        var newMap = newMap(map);
        return new BlueprintAnimation(animation.name(), animation.length(), newMap, blueprintScript, newMap.isEmpty() ? List.of(new AnimationMovement(0, null, null, null)) : newMap.values()
                .iterator()
                .next()
                .keyFrame()
                .stream()
                .map(a -> new AnimationMovement(a.time(), null, null, null))
                .toList());
    }

    private static Map<String, BlueprintAnimator> newMap(@NotNull Map<String, BlueprintAnimator.AnimatorData> oldMap) {
        var newMap = new HashMap<String, BlueprintAnimator>();
        Set<Float> floatSet = new TreeSet<>(Comparator.naturalOrder());
        oldMap.values().forEach(a -> a.points().stream().map(t -> t.position().time()).forEach(floatSet::add));
        for (Map.Entry<String, BlueprintAnimator.AnimatorData> entry : oldMap.entrySet()) {
            var list = getAnimationMovements(floatSet, entry);
            newMap.put(entry.getKey(), new BlueprintAnimator(
                    entry.getValue().name(),
                    entry.getValue().length(),
                    list
            ));
        }
        return newMap;
    }

    private static @NotNull List<AnimationMovement> getAnimationMovements(Set<Float> floatSet, Map.Entry<String, BlueprintAnimator.AnimatorData> entry) {
        var frame = entry.getValue().points();
        if (frame.isEmpty()) return Collections.emptyList();
        var list = VectorUtil.putAnimationPoint(frame, floatSet).stream().map(AnimationPoint::toMovement).collect(Collectors.toList());
        //reduceFrame(list);
        return processFrame(list);
    }

    private static void reduceFrame(@NotNull List<AnimationMovement> target) {
        var iterator = target.iterator();
        var l = 0F;
        var f = BetterModel.inst().configManager().keyframeThreshold();
        Vector3f beforeRot = new Vector3f();
        while (iterator.hasNext()) {
            var next = iterator.next();
            var rot = next.rotation();
            if (next.time() - l >= f || next.time() == 0 || (rot != null && new Vector3f(rot).sub(beforeRot).length() >= 45)) l = next.time();
            else {
                iterator.remove();
            }
            if (rot != null) beforeRot = rot;
        }
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
     * Gets loop iterator.
     * @return iterator
     */
    public BlueprintAnimator.AnimatorIterator emptyLoopIterator() {
        return new BlueprintAnimator.AnimatorIterator() {

            @NotNull
            @Override
            public AnimationMovement first() {
                return new AnimationMovement(1, null, null, null);
            }

            private int index = 0;

            @Override
            public int index() {
                return index;
            }

            @Override
            public void clear() {
                index = 0;
            }

            @Override
            public int lastIndex() {
                return emptyAnimator.size() - 1;
            }

            @Override
            public int length() {
                return Math.round(length * 100);
            }

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public AnimationMovement next() {
                if (index >= emptyAnimator.size()) index = 0;
                return emptyAnimator.get(index++);
            }
        };
    }

    /**
     * Gets single iterator.
     * @return iterator
     */
    public BlueprintAnimator.AnimatorIterator emptySingleIterator() {
        return new BlueprintAnimator.AnimatorIterator() {

            private int index = 0;

            @NotNull
            @Override
            public AnimationMovement first() {
                return new AnimationMovement(1, null, null, null);
            }

            @Override
            public int index() {
                return index;
            }

            @Override
            public int lastIndex() {
                return emptyAnimator.size() - 1;
            }

            @Override
            public void clear() {
                index = Integer.MAX_VALUE;
            }

            @Override
            public int length() {
                return Math.round(length * 100);
            }

            @Override
            public boolean hasNext() {
                return index < emptyAnimator.size();
            }

            @Override
            public AnimationMovement next() {
                return emptyAnimator.get(index++);
            }
        };
    }
}
