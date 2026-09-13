/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.bone;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.MathUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;

import static kr.toxicity.model.api.util.CollectionUtil.newSequencedAddressingMap;

/**
 * Bone IK solver
 */
@ApiStatus.Internal
@RequiredArgsConstructor
public final class BoneIKSolver {

    private static final int MAX_IK_ITERATION = 20;
    private static final Vector3f FROM_VECTOR = new Vector3f(0, -1, 0).normalize();

    private final Map<UUID, RenderedBone> boneMap;
    private final Object2ObjectLinkedOpenHashMap<RenderedBone, IKChain> locators = newSequencedAddressingMap();

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
        var chainArray = source.flatten()
            .filter(bone -> !bone.flattenBones().contains(locator) && bone.flattenBones().contains(target))
            .toArray(RenderedBone[]::new);
        if (chainArray.length < 2) return;
        locators.put(locator, new IKChain(chainArray));
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
        if (locators.isEmpty()) return;
        locators.object2ObjectEntrySet().fastForEach(entry -> {
            var locator = entry.getKey();
            var value = entry.getValue();
            fabrik(
                value.movements(uuid),
                value.invertedFirstRotation(uuid),
                value.cache.lengths,
                locator.state(uuid).after().position().get(value.cache.destination)
                    .add(locator.root.group.getPosition())
                    .sub(value.first().root.group.getPosition())
            );
        });
    }

    private record IKChain(@NotNull RenderedBone[] bones, @NotNull IKCache cache) {

        private IKChain(@NotNull RenderedBone[] bones) {
            this(bones, new IKCache(bones.length));
        }

        private @NotNull RenderedBone first() {
            return bones[0];
        }

        private @NotNull Quaternionf invertedFirstRotation(@Nullable UUID uuid) {
            return first().state(uuid).after().rotation().invert(cache.rotation);
        }

        private @NotNull BoneMovement[] movements(@Nullable UUID uuid) {
            var movements = cache.movements;
            for (int i = 0; i < bones.length; i++) {
                movements[i] = bones[i].state(uuid).after();
            }
            return movements;
        }
    }

    private record IKCache(@NotNull BoneMovement[] movements, float[] lengths, @NotNull Vector3f destination, @NotNull Quaternionf rotation) {
        private IKCache(int length) {
            this(new BoneMovement[length], new float[length - 1], new Vector3f(), new Quaternionf());
        }
    }

    private static void fabrik(@NotNull BoneMovement[] bones, @NotNull Quaternionf firstRot, float[] lengths, @NotNull Vector3f target) {
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
                var dist = current.distanceSquared(next);
                if (dist < MathUtil.VECTOR_COMPARISON_EPSILON_SQ) continue;
                InterpolationUtil.lerp(next, current, lengths[i] / (float) Math.sqrt(dist), current);
            }
            // Backward
            first.set(rootPos);
            for (int i = 0; i < bones.length - 1; i++) {
                var current = bones[i].position();
                var next = bones[i + 1].position();
                var dist = current.distanceSquared(next);
                if (dist < MathUtil.VECTOR_COMPARISON_EPSILON_SQ) continue;
                InterpolationUtil.lerp(current, next, lengths[i] / (float) Math.sqrt(dist), next);
            }
            // Check
            if (last.distanceSquared(target) < MathUtil.VECTOR_COMPARISON_EPSILON_SQ) break;
        }
        var rotCache = new Quaternionf();
        for (int i = 0; i < bones.length - 1; i++) {
            var current = bones[i];
            var next = bones[i + 1];

            var dir = next.position().sub(current.position(), vecCache);
            current.rotation().set(rotCache.identity().rotateTo(FROM_VECTOR, dir.normalize()).mul(firstRot).mul(current.rotation()));
        }
    }
}
