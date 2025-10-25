/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.blueprint.AnimationGenerator;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimator;
import kr.toxicity.model.api.data.blueprint.BlueprintChildren;
import kr.toxicity.model.api.script.AnimationScript;
import kr.toxicity.model.api.script.BlueprintScript;
import kr.toxicity.model.api.script.TimeScript;
import kr.toxicity.model.api.util.InterpolationUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static kr.toxicity.model.api.util.CollectionUtil.associate;

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
     * @param context context
     * @param availableUUIDs available uuids
     * @param children children
     * @return converted animation
     */
    public @NotNull BlueprintAnimation toBlueprint(
            @NotNull ModelLoadContext context,
            @NotNull Set<String> availableUUIDs,
            @NotNull List<BlueprintChildren> children
    ) {
        var animators = AnimationGenerator.createMovements(length(), children, associate(
                animators().entrySet().stream()
                        .filter(e -> availableUUIDs.contains(e.getKey()))
                        .map(Map.Entry::getValue)
                        .filter(ModelAnimator::isAvailable),
                e -> BoneTagRegistry.parse(e.name()),
                e -> {
                    var builder = new Builder(context, length());
                    e.stream().forEach(builder::addFrame);
                    return builder.build(name());
                }
        ));
        return new BlueprintAnimation(
                name(),
                loop(),
                length(),
                override(),
                animators,
                Optional.ofNullable(animators().get("effects"))
                        .filter(ModelAnimator::isNotEmpty)
                        .map(a -> toScript(a, context.placeholder))
                        .orElseGet(() -> BlueprintScript.fromEmpty(this)),
                animators.isEmpty() ? AnimationMovement.withEmpty(length()) : animators.values()
                        .iterator()
                        .next()
                        .keyframe()
                        .stream()
                        .map(AnimationMovement::empty)
                        .toList()
        );
    }

    private @NotNull BlueprintScript toScript(@NotNull ModelAnimator animator, @NotNull ModelPlaceholder placeholder) {
        var get = animator.stream()
                .filter(f -> f.point().hasScript())
                .map(d -> AnimationScript.of(Arrays.stream(placeholder.parseVariable(d.point().script()).split("\n"))
                        .map(BetterModel.plugin().scriptManager()::build)
                        .filter(Objects::nonNull)
                        .toList()
                ).time(d.time()))
                .toList();
        var list = new ArrayList<TimeScript>(get.size() + 2);
        if (get.getFirst().time() > 0) list.add(TimeScript.EMPTY);
        var before = 0F;
        for (TimeScript timeScript : get) {
            var t = timeScript.time();
            list.add(timeScript.time(InterpolationUtil.roundTime(t - before)));
            before = t;
        }
        var len = InterpolationUtil.roundTime(length() - before);
        if (len > 0) list.add(AnimationScript.EMPTY.time(len));
        return new BlueprintScript(
                name(),
                loop(),
                length(),
                list
        );
    }

    /**
     * Gets loop
     * @return loop
     */
    @Override
    public @NotNull AnimationIterator.Type loop() {
        return loop != null ? loop : AnimationIterator.Type.PLAY_ONCE;
    }

    /**
     * Gets animators
     * @return animators
     */
    @Override
    @NotNull
    public Map<String, ModelAnimator> animators() {
        return animators != null ? animators : Collections.emptyMap();
    }

    @RequiredArgsConstructor
    private static final class Builder {

        private final ModelLoadContext context;
        private final float length;

        private final List<VectorPoint> transform = new ArrayList<>();
        private final List<VectorPoint> scale = new ArrayList<>();
        private final List<VectorPoint> rotation = new ArrayList<>();

        void addFrame(@NotNull ModelKeyframe keyframe) {
            var time = keyframe.time();
            if (time > length) return;
            var interpolation = keyframe.findInterpolator();
            var function = keyframe.point().toFunction(context);
            var version = context.meta.formatVersion();
            switch (keyframe.channel()) {
                case POSITION -> transform.add(new VectorPoint(
                        function.map(version::convertAnimationPosition).memoize(),
                        time,
                        interpolation
                ));
                case ROTATION -> rotation.add(new VectorPoint(
                        function.map(version::convertAnimationRotation).memoize(),
                        time,
                        interpolation
                ));
                case SCALE -> scale.add(new VectorPoint(
                        function.map(vec -> vec.sub(1, 1, 1)).memoize(),
                        time,
                        interpolation
                ));
            }
        }

        @NotNull BlueprintAnimator.AnimatorData build(@NotNull String name) {
            return new BlueprintAnimator.AnimatorData(
                    name,
                    transform,
                    scale,
                    rotation
            );
        }
    }
}
