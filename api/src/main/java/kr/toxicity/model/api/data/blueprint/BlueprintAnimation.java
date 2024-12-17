package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public record BlueprintAnimation(@NotNull String name, @NotNull @Unmodifiable Map<String, BlueprintAnimator> animator) {
    public static @NotNull BlueprintAnimation from(@NotNull ModelAnimation animation) {
        var map = new HashMap<String, BlueprintAnimator>();
        for (Map.Entry<UUID, ModelAnimator> entry : animation.animators().entrySet()) {
            var builder = new BlueprintAnimator.Builder();
            var frameList = new ArrayList<>(entry.getValue().keyframes());
            frameList.sort(Comparator.naturalOrder());
            for (ModelKeyframe keyframe : frameList) {
                builder.addFrame(keyframe);
            }
            var name = entry.getValue().name();
            map.put(name, builder.build(name));
        }
        return new BlueprintAnimation(animation.name(), map);
    }
}
