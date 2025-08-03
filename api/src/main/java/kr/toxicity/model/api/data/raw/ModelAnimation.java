package kr.toxicity.model.api.data.raw;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.blueprint.AnimationGenerator;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimator;
import kr.toxicity.model.api.data.blueprint.BlueprintChildren;
import kr.toxicity.model.api.script.AnimationScript;
import kr.toxicity.model.api.script.BlueprintScript;
import kr.toxicity.model.api.script.TimeScript;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Raw animation of a model.
 * @param name name
 * @param loop whether to loop
 * @param uuid uuid
 * @param override override
 * @param length keyframe length
 * @param animators animators
 */
@ApiStatus.Internal
public record ModelAnimation(
        @NotNull String name,
        @Nullable AnimationIterator.Type loop,
        boolean override,
        @NotNull String uuid,
        float length,
        @Nullable Map<String, ModelAnimator> animators
) {
    /**
     * Converts raw animation to blueprint animation
     * @param children children
     * @param placeholder placeholder
     * @return converted animation
     */
    public @NotNull BlueprintAnimation toBlueprint(@NotNull List<BlueprintChildren> children, @NotNull ModelPlaceholder placeholder) {
        var map = new HashMap<BoneName, BlueprintAnimator.AnimatorData>();
        var blueprintScript = BlueprintScript.fromEmpty(this);
        var animator = animators();
        for (Map.Entry<String, ModelAnimator> entry : animator.entrySet()) {
            var name = entry.getValue().name();
            if (name == null) continue;
            if (entry.getKey().equals("effects")) {
                blueprintScript = toScript(entry.getValue());
            }
            else {
                var builder = new BlueprintAnimator.Builder(length());
                entry.getValue().keyframes()
                        .stream()
                        .sorted(Comparator.naturalOrder())
                        .forEach(keyframe -> builder.addFrame(keyframe, placeholder));
                map.put(BoneTagRegistry.parse(name), builder.build(name));
            }
        }
        var animators = AnimationGenerator.createMovements(length(), children, map);
        return new BlueprintAnimation(
                name(),
                loop(),
                length(),
                override(),
                animators,
                blueprintScript,
                animators.isEmpty() ? List.of(AnimationMovement.EMPTY, new AnimationMovement(length())) : animators.values()
                        .iterator()
                        .next()
                        .keyFrame()
                        .stream()
                        .map(a -> new AnimationMovement(a.time()))
                        .toList()
        );
    }

    private @NotNull BlueprintScript toScript(@NotNull ModelAnimator animator) {
        var get = animator.keyframes()
                .stream()
                .map(d -> AnimationScript.of(d.dataPoints()
                        .stream()
                        .map(Datapoint::script)
                        .filter(Objects::nonNull)
                        .map(raw -> BetterModel.plugin().scriptManager().build(raw))
                        .filter(Objects::nonNull)
                        .toList()
                ).time(d.time()))
                .toList();
        var before = animator.keyframes().getFirst().time();
        var list = new ArrayList<TimeScript>(get.size() + 2);
        list.add(AnimationScript.EMPTY.time(before));
        for (TimeScript timeScript : get) {
            var t = timeScript.time();
            list.add(timeScript.time(t - before));
            before = t;
        }
        list.add(AnimationScript.EMPTY.time(length() - before));
        return new BlueprintScript(
                name(),
                loop(),
                length(),
                list
        );
    }
    
    @Override
    public @NotNull AnimationIterator.Type loop() {
        return loop != null ? loop : AnimationIterator.Type.PLAY_ONCE;
    }

    @Override
    @NotNull
    public Map<String, ModelAnimator> animators() {
        return animators != null ? animators : Collections.emptyMap();
    }
}
