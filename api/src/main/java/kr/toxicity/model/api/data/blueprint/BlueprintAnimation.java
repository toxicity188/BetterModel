package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Collectors;

public record BlueprintAnimation(@NotNull String name, int length, @NotNull @Unmodifiable Map<String, BlueprintAnimator> animator) {
    public static @NotNull BlueprintAnimation from(@NotNull ModelAnimation animation) {
        var map = new HashMap<String, BlueprintAnimator>();
        var length = Math.round(animation.length() * 20);
        for (Map.Entry<UUID, ModelAnimator> entry : animation.animators().entrySet()) {
            var builder = new BlueprintAnimator.Builder(length);
            var frameList = new ArrayList<>(entry.getValue().keyframes());
            frameList.sort(Comparator.naturalOrder());
            for (ModelKeyframe keyframe : frameList) {
                builder.addFrame(keyframe);
            }
            var name = entry.getValue().name();
            map.put(name, builder.build(name));
        }
        return new BlueprintAnimation(animation.name(), length, newMap(map));
    }

    private static Map<String, BlueprintAnimator> newMap(@NotNull Map<String, BlueprintAnimator> oldMap) {
        var newMap = new HashMap<String, BlueprintAnimator>();
        var longSet = new TreeSet<Long>(Comparator.naturalOrder());
        oldMap.values().forEach(a -> a.keyFrame().stream().map(AnimationMovement::time).forEach(longSet::add));
        for (Map.Entry<String, BlueprintAnimator> entry : oldMap.entrySet()) {
            var list = getAnimationMovements(longSet, entry);
            newMap.put(entry.getKey(), new BlueprintAnimator(
                    entry.getValue().name(),
                    entry.getValue().length(),
                    list
            ));
        }
        return newMap;
    }

    private static @NotNull List<AnimationMovement> getAnimationMovements(Set<Long> longSet, Map.Entry<String, BlueprintAnimator> entry) {
        var frame = entry.getValue().keyFrame();
        var list = new ArrayList<AnimationMovement>();
        for (long l : longSet) {
            list.add(getFrame(frame, (int) l));
        }
        if (entry.getValue().name().equals("bone")) {
            System.out.println("DDD");
            for (AnimationMovement animationMovement : list) {
                System.out.println(animationMovement.time() + " : " + animationMovement.transform().y);
            }
        }
        return processFrame(list);
    }

    private static @NotNull AnimationMovement getFrame(@NotNull List<AnimationMovement> list, int i) {
        for (int t = 0; t < list.size(); t++) {
            var get = list.get(t);
            if (i <= get.time()) {
                if (t == 0 || get.time() == i) return get.set(i);
                else {
                    var get2 = list.get(t - 1);
                    return get2.plus(get.minus(get2).set(i - get2.time()));
                }
            }
        }
        return list.getFirst();
    }

    private static @NotNull List<AnimationMovement> processFrame(@NotNull List<AnimationMovement> target) {
        if (target.size() <= 1) return target;
        var list = new ArrayList<AnimationMovement>();
        for (int i = 1; i < target.size(); i++) {
            var get = target.get(i);
            list.add(get.time(get.time() - target.get(i - 1).time()));
        }
        return list;
    }
}
