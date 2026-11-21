/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet;
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
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class AnimationGeneratorTest {
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
    void findsAFullBezierTurnAndReturnWithEqualEndpoints() throws ReflectiveOperationException {
        var bone = group("bone", Float3.ZERO);
        var zero = new Vector3f();
        var rotation = List.of(
            point(0, 0, 0, 0, VectorInterpolator.BEZIER,
                new KeyframePoint.BezierConfig(zero, zero, new Vector3f(1F / 3, 0, 0), new Vector3f(480, 0, 0))),
            point(1, 0, 0, 0, VectorInterpolator.LINEAR,
                new KeyframePoint.BezierConfig(new Vector3f(-1F / 3, 0, 0), new Vector3f(480, 0, 0), zero, zero))
        );
        var frames = split(1, bone, data(bone, false, rotation));
        assertEquals(7, frames.size());
        assertSafeXAxisSegments(frames, rotation);
        var interpolator = InterpolationUtil.interpolatorFor(rotation);
        var angles = frames.stream().map(time -> interpolator.build(time).vector().eval().x).toList();
        var peak = angles.get(3);
        assertEquals(360F, peak, 2F);
        assertTrue(angles.get(1) < angles.get(2) && angles.get(2) < peak);
        assertTrue(peak > angles.get(4) && angles.get(4) > angles.get(5));
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
