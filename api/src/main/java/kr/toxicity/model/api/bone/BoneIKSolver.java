/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.MathUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bone IK solver
 */
@ApiStatus.Internal
@RequiredArgsConstructor
public final class BoneIKSolver {

    private static final int MAX_IK_ITERATION = 20;

    private final Map<UUID, RenderedBone> boneMap;
    private final Map<RenderedBone, IKTree> locators = new LinkedHashMap<>();

    /**
     * Adds some external locator to this solver
     * @param ikSource nullable source
     * @param ikTarget target bone
     * @param locator locator bone
     */
    public void addLocator(@Nullable UUID ikSource, @NotNull UUID ikTarget, @NotNull RenderedBone locator) {
        var target = boneMap.get(ikTarget);
        if (target == null) return;
        var source = ikSource == null ? target.root : boneMap.getOrDefault(ikSource, target.root);
        var list = source.flatten()
                .filter(bone -> !bone.flattenBones().contains(locator) && bone.flattenBones().contains(target))
                .toList();
        if (list.size() < 2) return;
        locators.put(locator, new IKTree(source, list, new float[list.size() - 1]));
    }

    /**
     * Solves ik
     */
    public void solve() {
        solve(null);
    }

    /**
     * Solves ik
     * @param uuid player uuid
     */
    public void solve(@Nullable UUID uuid) {
        for (var entry : locators.entrySet()) {
            var locator = entry.getKey();
            var value = entry.getValue();
            var root = value.bones.getFirst();
            fabrik(
                    value.bones.stream()
                            .map(bone -> bone.state(uuid).after())
                            .toList(),
                    value.source.state(uuid).after().rotation().invert(new Quaternionf()),
                    value.buffer,
                    locator.state(uuid).after().position().get(new Vector3f())
                            .add(locator.root.group.getPosition())
                            .sub(root.state(uuid).after().position())
                            .sub(root.root.group.getPosition())
            );
        }
    }

    private record IKTree(@NotNull RenderedBone source, @NotNull List<RenderedBone> bones, float[] buffer) {}

    private static void fabrik(@NotNull List<BoneMovement> bones, @NotNull Quaternionf parentRot, float[] lengths, @NotNull Vector3f target) {
        var first = bones.getFirst().position();
        var last = bones.getLast().position();

        var rootPos = new Vector3f(first);

        for (int i = 0; i < bones.size() - 1; i++) {
            var before = bones.get(i);
            var after = bones.get(i + 1);
            lengths[i] = before.position().distance(after.position());
        }
        for (int iter = 0; iter < MAX_IK_ITERATION; iter++) {
            // Forward
            last.set(target);
            for (int i = bones.size() - 2; i >= 0; i--) {
                var current = bones.get(i).position();
                var next = bones.get(i + 1).position();
                var dist = current.distance(next);
                if (dist < MathUtil.FLOAT_COMPARISON_EPSILON) continue;
                InterpolationUtil.lerp(next, current, lengths[i] / dist, current);
            }
            // Backward
            first.set(rootPos);
            for (int i = 0; i < bones.size() - 1; i++) {
                var current = bones.get(i).position();
                var next = bones.get(i + 1).position();
                var dist = current.distance(next);
                if (dist < MathUtil.FLOAT_COMPARISON_EPSILON) continue;
                InterpolationUtil.lerp(current, next, lengths[i] / dist, next);
            }
            // Check
            if (last.distance(target) < MathUtil.FRAME_EPSILON) break;
        }
        for (int i = 0; i < bones.size() - 1; i++) {
            var current = bones.get(i);
            var next = bones.get(i + 1);

            var dir = next.position().sub(current.position(), new Vector3f());
            current.rotation().set(MathUtil.fromToRotation(dir).mul(parentRot).mul(current.rotation()));
        }
    }
}
