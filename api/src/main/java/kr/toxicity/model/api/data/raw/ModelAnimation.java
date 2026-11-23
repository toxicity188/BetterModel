/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.floats.Float2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectMap;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationProgress;
import kr.toxicity.model.api.animation.keyframe.KeyframeDatapoint;
import kr.toxicity.model.api.animation.keyframe.KeyframePoint;
import kr.toxicity.model.api.data.blueprint.AnimationGenerator;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimator;
import kr.toxicity.model.api.data.blueprint.BlueprintElement;
import kr.toxicity.model.api.manager.ScriptManager;
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
 * Represents a raw animation definition from a model file.
 * <p>
 * This record holds the properties of an animation, such as its name, length, loop mode,
 * and the animators that define the keyframes for each bone group.
 * </p>
 *
 * @param name the name of the animation
 * @param loop the loop mode (e.g., play_once, loop, hold)
 * @param override whether this animation should override others
 * @param uuid the unique identifier of the animation
 * @param length the total length of the animation in seconds
 * @param blendWeight the optional numeric or Molang blend weight; null defaults to one
 * @param animators a map of animators, keyed by the UUID of the bone group they affect
 * @since 1.15.2
 */
@ApiStatus.Internal
public record ModelAnimation(
    @NotNull String name,
    @Nullable AnimationIterator.Type loop,
    boolean override,
    @NotNull String uuid,
    float length,
    @Nullable @SerializedName("blend_weight") JsonPrimitive blendWeight,
    @Nullable Map<String, ModelAnimator> animators
) {

    /**
     * Supplies model loading state and blend weight while converting animation keyframes.
     * <pre>{@code
     * var context = new ModelAnimation.AnimationContext(model, KeyframeDatapoint.ONE);
     * }</pre>
     *
     * @param model the model loading context
     * @param blendWeight the animation blend weight
     * @since 3.7.0
     */
    public record AnimationContext(
        @NotNull ModelLoadContext model,
        @NotNull KeyframeDatapoint blendWeight
    ) {}

    /**
     * Converts this raw animation data into a processed {@link BlueprintAnimation}.
     * The transform timeline uses the tick-rounded duration, and effect keyframes are scheduled on the same tick grid.
     * A null or blank blend weight defaults to one. Other values are applied to transform keyframes.
     * <pre>{@code
     * var blueprint = animation.toBlueprint(loadContext, roots);
     * }</pre>
     *
     * @param context the model loading context
     * @param elements the list of root blueprint elements
     * @return the blueprint animation
     * @since 1.15.2
     */
    public @NotNull BlueprintAnimation toBlueprint(
        @NotNull ModelLoadContext context,
        @NotNull List<BlueprintElement> elements
    ) {
        var ctx = new AnimationContext(context, KeyframeDatapoint.fromJson(blendWeight(), KeyframeDatapoint.ONE, context.placeholder));
        var animators = AnimationGenerator.generate(tickTime(), elements, associate(
            animators().entrySet().stream()
                .filter(e -> ctx.model.availableUUIDs.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .filter(ModelAnimator::isAvailable)
                .map(a -> buildAnimationData(ctx, a)),
            BlueprintAnimator.AnimatorData::name
        ));
        return new BlueprintAnimation(
            name(),
            loop(),
            tickTime(),
            override(),
            animators,
            Optional.ofNullable(animators().get("effects"))
                .filter(ModelAnimator::isNotEmpty)
                .map(a -> toScript(a, context.placeholder))
                .orElseGet(() -> BlueprintScript.fromEmpty(this)),
            animators.isEmpty() ? AnimationProgress.emptyStorage(tickTime()) : animators.values()
                .iterator()
                .next()
                .keyframe()
                .toEmpty()
        );
    }

    /**
     * Builds an effect timeline from tick-rounded times and stores successive delays in seconds.
     *
     * @param animator the raw effect track
     * @param placeholder the model placeholder used to resolve script variables
     * @return the effect script with its original duration metadata
     */
    private @NotNull BlueprintScript toScript(@NotNull ModelAnimator animator, @NotNull ModelPlaceholder placeholder) {
        var set = new Float2ObjectAVLTreeMap<List<AnimationScript>>();
        set.computeIfAbsent(0F, _ -> new ArrayList<>()).add(AnimationScript.EMPTY);
        set.computeIfAbsent(tickTime(), _ -> new ArrayList<>()).add(AnimationScript.EMPTY);
        animator.stream()
            .filter(f -> f.point().hasScript())
            .forEach(d -> Arrays.stream(d.point().script().split("\n"))
                .map(placeholder)
                .map(BetterModel.platform().manager(ScriptManager.class)::build)
                .filter(Objects::nonNull)
                .forEach(script -> set.computeIfAbsent(d.tickTime(), _ -> new ArrayList<>()).add(script)));
        var array = new TimeScript[set.size()];
        var before = 0F;
        var i = 0;
        for (Float2ObjectMap.Entry<List<AnimationScript>> entry : set.float2ObjectEntrySet()) {
            var t = entry.getFloatKey();
            array[i++] = AnimationScript.of(entry.getValue()).time(t - before);
            before = t;
        }
        return new BlueprintScript(
            name(),
            loop(),
            length(),
            List.of(array)
        );
    }

    /**
     * Returns the animation length rounded to the nearest 50 ms Minecraft tick.
     * <pre>{@code
     * float playbackLength = animation.tickTime();
     * }</pre>
     *
     * @return the rounded length in seconds
     * @since 3.7.2
     */
    public float tickTime() {
        return InterpolationUtil.roundTick(length());
    }

    /**
     * Returns the loop mode of the animation.
     *
     * @return the loop mode, defaulting to {@link AnimationIterator.Type#PLAY_ONCE} if null
     * @since 1.15.2
     */
    @Override
    public @NotNull AnimationIterator.Type loop() {
        return loop != null ? loop : AnimationIterator.Type.PLAY_ONCE;
    }

    /**
     * Returns the map of animators for this animation.
     *
     * @return the animators, or an empty map if null
     * @since 1.15.2
     */
    @Override
    @NotNull
    public Map<String, ModelAnimator> animators() {
        return animators != null ? animators : Collections.emptyMap();
    }

    @NotNull
    private BlueprintAnimator.AnimatorData buildAnimationData(@NotNull AnimationContext context, @NotNull ModelAnimator animator) {
        var position = new ArrayList<KeyframePoint>();
        var rotation = new ArrayList<KeyframePoint>();
        var scale = new ArrayList<KeyframePoint>();
        var version = context.model.meta.formatVersion();
        animator.stream().filter(keyframe -> keyframe.time() <= tickTime()).forEach(keyframe -> {
            switch (keyframe.channel()) {
                case POSITION -> position.add(keyframe.point(context, version.positionAxis()));
                case ROTATION -> rotation.add(keyframe.point(context, version.rotationAxis()));
                case SCALE -> scale.add(keyframe.point(context, version.scaleAxis()));
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
