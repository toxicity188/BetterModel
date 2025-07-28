package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.script.BlueprintScript;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        @Nullable BlueprintScript script,
        @NotNull List<AnimationMovement> emptyAnimator
) {
    /**
     * Converts from raw animation.
     * @param animation raw animation
     * @return converted animation
     */
    public static @NotNull BlueprintAnimation from(@NotNull List<BlueprintChildren> children, @NotNull ModelAnimation animation) {
        var map = new HashMap<BoneName, BlueprintAnimator.AnimatorData>();
        var blueprintScript = animation.override() ? null : BlueprintScript.fromEmpty(animation);
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
        var animators = AnimationGenerator.createMovements(animation.length(), children, map);
        return new BlueprintAnimation(
                animation.name(),
                animation.loop(),
                animation.length(),
                animation.override(),
                animators,
                blueprintScript,
                animators.isEmpty() ? List.of(AnimationMovement.EMPTY, new AnimationMovement(animation.length())) : animators.values()
                        .iterator()
                        .next()
                        .keyFrame()
                        .stream()
                        .map(a -> new AnimationMovement(a.time()))
                        .toList()
        );
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
