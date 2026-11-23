/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.floats.FloatSortedSet;
import kr.toxicity.model.api.animation.keyframe.Keyframe;
import kr.toxicity.model.api.animation.keyframe.KeyframePoint;
import kr.toxicity.model.api.animation.keyframe.KeyframeVector;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.data.Float3;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimator.AnimatorData;
import kr.toxicity.model.api.data.blueprint.BlueprintElement.Group;
import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.interpolator.VectorInterpolator;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class AnimationGeneratorTest {
    @Test
    void interpolatesEarlierSegmentsAfterLaterSamples() {
        var sampler = InterpolationUtil.interpolatorFor(List.of(
            point(0, 0, 0, 0), point(1, 360, 0, 0), point(2, 0, 0, 0)
        ));
        assertEquals(90F, sampler.build(1.75F).vector().eval().x);
        assertEquals(90F, sampler.build(0.25F).vector().eval().x);
        assertEquals(360F, sampler.build(1F).vector().eval().x);
    }

    @Test
    void samplesAndInsertsEveryRequiredTick() throws ReflectiveOperationException {
        var bone = group("bone", Float3.ZERO);
        for (int ticks : new int[]{1, 2, 3, 5, 14}) {
            var end = ticks * MathUtil.MINECRAFT_TICK_SECONDS;
            var frames = split(end, bone, data(bone, false, axis(end, ticks * 150))).toFloatArray();
            assertEquals(ticks + 1, frames.length);
            for (int i = 0; i <= ticks; i++) assertEquals(i * MathUtil.MINECRAFT_TICK_SECONDS, frames[i]);
        }
    }

    @Test
    void acceptsNearTickGapsAndKeepsOffGridEndpoints() throws ReflectiveOperationException {
        var bone = group("bone", Float3.ZERO);
        for (float end : new float[]{0.09999999F, 0.13F}) {
            var frames = split(end, bone, data(bone, false, axis(end, 270))).toFloatArray();
            assertArrayEquals(new float[]{0F, 0.05F, end}, frames);
        }
        var rotation = List.of(point(0.13F, 0, 0, 0), point(0.26F, 270, 0, 0));
        var frames = split(0.26F, bone, data(bone, false, rotation)).toFloatArray();
        assertArrayEquals(new float[]{0F, 0.13F, 0.2F, 0.26F}, frames, Arrays.toString(frames));
    }

    @Test
    void composingAStaticChildKeepsCachedParentAndEarlierPosesIntact() throws ReflectiveOperationException {
        var child = group("child", new Float3(90, 0, 0));
        var parent = group("parent", Float3.ZERO, child);
        var rotation = List.of(KeyframePoint.EMPTY, point(1, 0, 90, 0));
        var ctor = AnimationGenerator.class.getDeclaredConstructor(Function.class, List.class);
        ctor.setAccessible(true);
        Function<Group, AnimatorData> lookup = group -> group == parent ? data(parent, false, rotation) : null;
        var generator = ctor.newInstance(lookup, List.of(parent));
        var method = AnimationGenerator.class.getDeclaredMethod("samplerFunction", FloatArrayList.class);
        method.setAccessible(true);
        var sampler = (Int2ObjectFunction<?>) method.invoke(generator, new FloatArrayList(new float[]{0, 1}));
        var first = (Quaternionf[]) sampler.get(0);
        var snapshot = new Quaternionf(first[1]);
        var last = (Quaternionf[]) sampler.get(1);
        assertNotSame(first[1], last[1]);
        assertEquals(snapshot, first[1]);
        assertSame(first, sampler.get(0));
        var parentEnd = MathUtil.toQuaternion(new Vector3f(0, 90, 0));
        assertEquals(parentEnd, last[0]);
        assertEquals(parentEnd.mul(snapshot), last[1]);
    }

    @Test
    void samplesTimeDependentRotationEvenWhenBothEndpointsAreZero() throws ReflectiveOperationException {
        var bone = group("bone", Float3.ZERO);
        var pulse = new KeyframePoint(context -> KeyframeVector.of(
            Keyframe.of(context.animTime() == 0.25F ? 160F : 0F), Keyframe.ZERO, Keyframe.ZERO
        ), 0F, KeyframePoint.EMPTY.bezier(), VectorInterpolator.LINEAR);
        var frames = split(1F, bone, data(bone, false, List.of(pulse, point(1F, 0, 0, 0))));
        assertTrue(frames.contains(0.25F));
    }

    @Test
    void insertsTheMinimumForSingleAxisTurns() throws ReflectiveOperationException {
        var bone = group("bone", Float3.ZERO);
        for (int angle : new int[]{90, 179, 180, 270, 360}) {
            var rotation = axis(1, angle);
            var frames = split(1, bone, data(bone, false, rotation));
            var count = angle >= 360 ? 2 : angle >= 180 ? 1 : 0;
            assertEquals(count + 2, frames.size(), () -> "Angle " + angle + ": " + frames);
            assertSafeXAxisSegments(frames, rotation);
        }
    }

    @Test
    void keepsTheSignedTurnsOnBothSidesOfAnExplicitKeyframe() throws ReflectiveOperationException {
        var bone = group("bone", Float3.ZERO);
        var rotation = List.of(KeyframePoint.EMPTY, point(1, 360, 0, 0), point(2, 0, 0, 0));
        var frames = split(2, bone, data(bone, false, rotation));
        assertEquals(7, frames.size());
        assertSafeXAxisSegments(frames, rotation);

        var interpolator = InterpolationUtil.interpolatorFor(rotation);
        var previous = 0F;
        for (var time : frames) {
            if (time == 0F) continue;
            var angle = interpolator.build(time).vector().eval().x;
            assertTrue(time <= 1F ? angle > previous : angle < previous);
            previous = angle;
        }
    }

    @Test
    void composesParentFirstAndRespectsGlobalRotation() throws ReflectiveOperationException {
        var child = group("child", Float3.ZERO);
        var parent = group("parent", Float3.ZERO, child);
        var parentData = data(parent, false, List.of(KeyframePoint.EMPTY, point(1, 0, 0, 155)));
        var rotation = List.of(KeyframePoint.EMPTY, point(1, 120, 0, 60));
        assertEquals(3, split(1, parent, parentData, data(child, false, rotation)).size());
        assertEquals(2, split(1, parent, parentData, data(child, true, rotation)).size());
    }

    @Test
    void ignoresLargeEulerChangesThatCancelAtTheBindPose() throws ReflectiveOperationException {
        var bone = group("bone", new Float3(0, 90, 0));
        var rotation = List.of(KeyframePoint.EMPTY, point(1, 720, 0, 720));
        assertEquals(2, split(1, bone, data(bone, false, rotation)).size());
    }

    @Test
    void findsOneUnevenSplitWhenTheTimeMidpointWouldReverse() throws ReflectiveOperationException {
        var bone = group("bone", Float3.ZERO);
        var zero = new Vector3f();
        var rotation = List.of(
            point(0, 0, 0, 0, VectorInterpolator.BEZIER,
                new KeyframePoint.BezierConfig(zero, zero, new Vector3f(1F / 3, 0, 0), new Vector3f(270, 0, 0))),
            point(1, 270, 0, 0, VectorInterpolator.LINEAR,
                new KeyframePoint.BezierConfig(new Vector3f(-1F / 3, 0, 0), zero, zero, zero))
        );
        var interpolate = InterpolationUtil.interpolatorFor(rotation);
        assertTrue(interpolate.build(0.5F).vector().eval().x > 180F);
        var frames = split(1, bone, data(bone, false, rotation));
        assertEquals(3, frames.size());
        assertSafeXAxisSegments(frames, rotation);
    }

    @Test
    void usesSharedTimesAndKeepsAtLeastOneTickBetweenNewFrames() throws ReflectiveOperationException {
        var child = group("child", Float3.ZERO);
        var parent = group("parent", Float3.ZERO, child);
        var rotation = axis(0.1F, 270);
        assertArrayEquals(new float[]{0, 0.05F, 0.1F},
            split(0.1F, parent, data(parent, false, rotation), data(child, true, rotation)).toFloatArray());
        assertEquals(2, split(0.075F, parent, data(parent, false, axis(0.075F, 270))).size());
    }

    @Test
    void sharesTheExistingStepFrameTime() throws ReflectiveOperationException {
        var bone = group("bone", Float3.ZERO);
        var rotation = List.of(
            point(0, 0, 0, 0, VectorInterpolator.STEP, KeyframePoint.EMPTY.bezier()), point(1, 270, 0, 0)
        );
        assertArrayEquals(new float[]{0F, 0.95F, 1F}, split(1, bone, data(bone, false, rotation)).toFloatArray());
    }

    private static void assertSafeXAxisSegments(FloatSortedSet frames, List<KeyframePoint> rotation) {
        var interpolate = InterpolationUtil.interpolatorFor(rotation);
        var previous = 0F;
        for (var time : frames) {
            var angle = interpolate.build(time).vector().eval().x;
            assertTrue(Math.abs(angle - previous) < 179.9F, () -> "Unsafe frame at " + time);
            previous = angle;
        }
    }

    private static FloatSortedSet split(float length, Group root, AnimatorData... data) throws ReflectiveOperationException {
        var constructor = AnimationGenerator.class.getDeclaredConstructor(Function.class, List.class);
        constructor.setAccessible(true);
        Function<Group, AnimatorData> lookup = group -> Arrays.stream(data)
            .filter(animator -> animator.name().equals(group.name())).findFirst().orElse(null);
        var generator = constructor.newInstance(lookup, List.of(root));
        var frames = new FloatAVLTreeSet(MathUtil.FRAME_COMPARATOR);
        Arrays.stream(data).flatMap(AnimatorData::allPoints).forEach(point -> frames.add(point.time()));
        frames.add(0F);
        frames.add(length);
        generator.interpolateRotation(frames);
        generator.interpolateStep(frames);
        return frames;
    }

    private static Group group(String name, Float3 rotation, BlueprintElement... children) {
        return new Group(UUID.randomUUID(), new BoneName(Set.of(), name, name), Float3.ZERO, rotation, List.of(children), true);
    }

    private static AnimatorData data(Group bone, boolean global, List<KeyframePoint> rotation) {
        return new AnimatorData(bone.name(), List.of(), List.of(), rotation, global);
    }

    private static List<KeyframePoint> axis(float time, float angle) {
        return List.of(KeyframePoint.EMPTY, point(time, angle, 0, 0));
    }

    private static KeyframePoint point(float time, float x, float y, float z) {
        return point(time, x, y, z, VectorInterpolator.LINEAR, KeyframePoint.EMPTY.bezier());
    }

    private static KeyframePoint point(float time, float x, float y, float z,
                                       VectorInterpolator interpolation, KeyframePoint.BezierConfig handles) {
        return new KeyframePoint(_ -> KeyframeVector.of(Keyframe.of(x), Keyframe.of(y), Keyframe.of(z)),
            time, handles, interpolation);
    }
}
