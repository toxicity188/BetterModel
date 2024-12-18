package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

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
        return new BlueprintAnimation(animation.name(), length, map);
    }

    private static Map<String, BlueprintAnimator> newMap(long length, @NotNull Map<String, BlueprintAnimator> oldMap) {
        var newMap = new HashMap<String, BlueprintAnimator>();
        for (Map.Entry<String, BlueprintAnimator> entry : oldMap.entrySet()) {
            var list = getAnimationMovements(length, entry);
            newMap.put(entry.getKey(), new BlueprintAnimator(
                    entry.getValue().name(),
                    entry.getValue().length(),
                    list
            ));
        }
        return newMap;
    }

    private static @NotNull ArrayList<AnimationMovement> getAnimationMovements(long length, Map.Entry<String, BlueprintAnimator> entry) {
        var intSet = new TreeSet<Long>(Comparator.naturalOrder());
        entry.getValue().keyFrame().stream().map(AnimationMovement::time).forEach(intSet::add);
        intSet.add(length);
        var frame = entry.getValue().keyFrame();
        var list = new ArrayList<AnimationMovement>();
        for (long l : intSet) {
            for (AnimationMovement animationMovement : frame) {
                if (l <= animationMovement.time()) {
                    System.out.println(animationMovement.time() + " : " + l);
                    list.add(animationMovement.set(l));
                    break;
                }
            }
        }
        return list;
    }
}
