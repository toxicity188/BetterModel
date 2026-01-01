/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
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
import kr.toxicity.model.api.data.blueprint.BlueprintElement;
import kr.toxicity.model.api.script.AnimationScript;
import kr.toxicity.model.api.script.BlueprintScript;
import kr.toxicity.model.api.script.TimeScript;
import kr.toxicity.model.api.util.InterpolationUtil;
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
     * @param children children
     * @return converted animation
     */
    public @NotNull BlueprintAnimation toBlueprint(
        @NotNull ModelLoadContext context,
        @NotNull List<BlueprintElement> children
    ) {
        var animators = AnimationGenerator.createMovements(length(), children, associate(
            animators().entrySet().stream()
                .filter(e -> context.availableUUIDs.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .filter(ModelAnimator::isAvailable)
                .map(a -> buildAnimationData(context, a)),
            data -> BoneTagRegistry.parse(data.name())
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

    private @Nullable BlueprintScript toScript(@NotNull ModelAnimator animator, @NotNull ModelPlaceholder placeholder) {
        var get = animator.stream()
            .filter(f -> f.point().hasScript())
            .map(d -> AnimationScript.of(Arrays.stream(placeholder.parseVariable(d.point().script()).split("\n"))
                .map(BetterModel.plugin().scriptManager()::build)
                .filter(Objects::nonNull)
                .toList()
            ).time(d.time()))
            .toList();
        if (get.isEmpty()) return null;
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

    @NotNull
    private BlueprintAnimator.AnimatorData buildAnimationData(@NotNull ModelLoadContext context, @NotNull ModelAnimator animator) {
        var position = new ArrayList<VectorPoint>();
        var rotation = new ArrayList<VectorPoint>();
        var scale = new ArrayList<VectorPoint>();
        var version = context.meta.formatVersion();
        animator.stream().filter(keyframe -> keyframe.time() <= length()).forEach(keyframe -> {
            switch (keyframe.channel()) {
                case POSITION -> position.add(keyframe.point(context, version::convertAnimationPosition));
                case ROTATION -> rotation.add(keyframe.point(context, version::convertAnimationRotation));
                case SCALE -> scale.add(keyframe.point(context, version::convertAnimationScale));
            }
        });
        return new BlueprintAnimator.AnimatorData(
            animator.name(),
            position,
            scale,
            rotation,
            animator.rotationGlobal()
        );
    }
}
