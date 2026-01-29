/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
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
import java.util.Map;
import java.util.UUID;

/**
 * Bone IK solver
 */
@ApiStatus.Internal
@RequiredArgsConstructor
public final class BoneIKSolver {

    private static final int MAX_IK_ITERATION = 20;
    private static final float DISTANCE_THRESHOLD_SQ = MathUtil.FRAME_EPSILON * MathUtil.FRAME_EPSILON;

    private final Map<UUID, RenderedBone> boneMap;
    private final Map<RenderedBone, IKChain> locators = new LinkedHashMap<>();

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
            .toArray(RenderedBone[]::new);
        if (list.length < 2) return;
        locators.put(locator, new IKChain(source, list, new IKCache(list.length)));
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
            var root = value.first();
            fabrik(
                value.movements(uuid),
                value.invertedParentRotation(uuid),
                value.cache.buffer,
                locator.state(uuid).after().position().get(value.cache.destination)
                    .add(locator.root.group.getPosition())
                    .sub(root.state(uuid).after().position())
                    .sub(root.root.group.getPosition())
            );
        }
    }

    private record IKChain(@NotNull RenderedBone source, @NotNull RenderedBone[] bones, @NotNull IKCache cache) {

        private @NotNull RenderedBone first() {
            return bones[0];
        }

        private @NotNull Quaternionf invertedParentRotation(@Nullable UUID uuid) {
            return source.state(uuid).after().rotation().invert(cache.rotation);
        }

        private @NotNull BoneMovement[] movements(@Nullable UUID uuid) {
            var movements = cache.movements;
            for (int i = 0; i < bones.length; i++) {
                movements[i] = bones[i].state(uuid).after();
            }
            return movements;
        }
    }

    private record IKCache(@NotNull BoneMovement[] movements, float[] buffer, @NotNull Vector3f destination, @NotNull Quaternionf rotation) {
        private IKCache(int length) {
            this(new BoneMovement[length], new float[length - 1], new Vector3f(), new Quaternionf());
        }
    }

    private static void fabrik(@NotNull BoneMovement[] bones, @NotNull Quaternionf parentRot, float[] lengths, @NotNull Vector3f target) {
        var first = bones[0].position();
        var last = bones[bones.length - 1].position();

        var vecCache = new Vector3f();
        var rootPos = first.get(vecCache);

        for (int i = 0; i < bones.length - 1; i++) {
            var before = bones[i];
            var after = bones[i + 1];
            lengths[i] = before.position().distance(after.position());
        }
        for (int iter = 0; iter < MAX_IK_ITERATION; iter++) {
            // Forward
            last.set(target);
            for (int i = bones.length - 2; i >= 0; i--) {
                var current = bones[i].position();
                var next = bones[i + 1].position();
                var dist = current.distance(next);
                if (dist < MathUtil.FLOAT_COMPARISON_EPSILON) continue;
                InterpolationUtil.lerp(next, current, lengths[i] / dist, current);
            }
            // Backward
            first.set(rootPos);
            for (int i = 0; i < bones.length - 1; i++) {
                var current = bones[i].position();
                var next = bones[i + 1].position();
                var dist = current.distance(next);
                if (dist < MathUtil.FLOAT_COMPARISON_EPSILON) continue;
                InterpolationUtil.lerp(current, next, lengths[i] / dist, next);
            }
            // Check
            if (last.distanceSquared(target) < DISTANCE_THRESHOLD_SQ) break;
        }
        var rotCache = new Quaternionf();
        for (int i = 0; i < bones.length - 1; i++) {
            var current = bones[i];
            var next = bones[i + 1];

            var dir = next.position().sub(current.position(), vecCache);
            current.rotation().set(MathUtil.fromToRotation(dir.normalize(), rotCache).mul(parentRot).mul(current.rotation()));
        }
    }
}
