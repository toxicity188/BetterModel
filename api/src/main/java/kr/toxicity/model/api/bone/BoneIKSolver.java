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
import static kr.toxicity.model.api.util.MathUtil.fma;

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
    private final FabrikCache runtimeCache = new FabrikCache();

    /**
     * Adds some external locator to this solver
     * @param config ik chain config
     * @param locator locator bone
     */
    public void addLocator(@NotNull BoneIKChainConfig config, @NotNull RenderedBone locator) {
        var target = boneMap.get(config.ikTarget());
        if (target == null) return;
        var ikSource = config.ikSource();
        var source = ikSource == null ? target.root : boneMap.getOrDefault(ikSource, target.root);
        var chainArray = source.flatten()
            .filter(bone -> !bone.flattenBones().contains(locator) && bone.flattenBones().contains(target))
            .toArray(RenderedBone[]::new);
        if (chainArray.length < 2) return;
        var ikPole = config.ikPole();
        locators.put(locator, new IKChain(ikPole != null ? boneMap.get(ikPole) : null, chainArray, config.lockIkTargetRotation()));
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
                value.pole(uuid),
                value.movements(uuid),
                value.invertedFirstRotation(uuid),
                value.cache.lengths,
                locator.state(uuid).after().position().get(value.cache.destination)
                    .add(locator.rootPosition())
                    .sub(value.rootPosition()),
                value.lockIkTargetRotation,
                runtimeCache
            );
        });
    }

    private record IKChain(@Nullable RenderedBone pole, @NotNull RenderedBone[] bones, boolean lockIkTargetRotation, @NotNull IKCache cache) {

        private IKChain(@Nullable RenderedBone pole, @NotNull RenderedBone[] bones, boolean lockIkTargetRotation) {
            this(pole, bones, lockIkTargetRotation, new IKCache(bones.length));
        }

        private @Nullable Vector3f pole(@Nullable UUID uuid) {
            return pole != null ? pole.state(uuid).after().position().get(cache.pole)
                .add(pole.rootPosition())
                .sub(rootPosition()): null;
        }

        private @NotNull RenderedBone first() {
            return bones[0];
        }

        private @NotNull Vector3f rootPosition() {
            return first().rootPosition();
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

    private record IKCache(
        @NotNull BoneMovement[] movements,
        float[] lengths,
        @NotNull Vector3f destination,
        @NotNull Vector3f pole,
        @NotNull Quaternionf rotation
    ) {
        private IKCache(int length) {
            this(new BoneMovement[length], new float[length - 1], new Vector3f(), new Vector3f(), new Quaternionf());
        }
    }

    private record FabrikCache(
        @NotNull Vector3f position,
        @NotNull Quaternionf rotation,
        @NotNull Vector3f chain,
        @NotNull Vector3f blend,
        @NotNull Vector3f destination,
        @NotNull Vector3f temp
    ) {
        private FabrikCache() {
            this(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(),
                new Vector3f(),
                new Vector3f(),
                new Vector3f()
            );
        }
    }

    private static void fabrik(
        @Nullable Vector3f pole,
        @NotNull BoneMovement[] bones,
        @NotNull Quaternionf firstRot,
        float[] lengths,
        @NotNull Vector3f target,
        boolean lockIkTargetRotation,
        @NotNull FabrikCache cache
    ) {
        var first = bones[0].position();
        var last = bones[bones.length - 1].position();

        var rootPos = first.get(cache.position);

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

        // Pole
        if (pole != null) pole(pole, bones, cache);

        for (int i = 0; i < bones.length - 1; i++) {
            var current = bones[i];
            var next = bones[i + 1];

            var destRot = cache.rotation.identity()
                .rotateTo(FROM_VECTOR, next.position().sub(current.position(), cache.position).normalize())
                .mul(firstRot);

            var currentRot = current.rotation();
            destRot.mul(currentRot, currentRot);
        }

        if (lockIkTargetRotation) return;
        var lastRot = bones[bones.length - 1].rotation();
        cache.rotation.mul(lastRot, lastRot);
    }

    private static void pole(@NotNull Vector3f polePosition, @NotNull BoneMovement[] bones, @NotNull FabrikCache cache) {
        var rootPos = bones[0].position();
        var endPos = bones[bones.length - 1].position();

        // Calculate chain axis
        endPos.sub(rootPos, cache.chain);
        if (cache.chain.lengthSquared() < MathUtil.VECTOR_COMPARISON_EPSILON_SQ) return;
        cache.chain.normalize();

        // 1. Current bend direction from the middle joint
        var currentBendDir = projectOntoPlane(rootPos, bones[bones.length / 2].position(), cache.chain, cache.blend);
        if (currentBendDir.lengthSquared() < MathUtil.VECTOR_COMPARISON_EPSILON_SQ) return;

        // 2. Target bend direction towards the pole
        var targetPoleDir = projectOntoPlane(rootPos, polePosition, cache.chain, cache.destination);
        if (targetPoleDir.lengthSquared() < MathUtil.VECTOR_COMPARISON_EPSILON_SQ) return;

        // 3. Rotation to align current bend to target
        var poleRotation = cache.rotation.identity().rotateTo(currentBendDir.normalize(), targetPoleDir.normalize());

        // 4. Apply rotation to middle joints
        for (int i = 1; i < bones.length - 1; i++) {
            var pos = bones[i].position();
            rootPos.add(pos.sub(rootPos, cache.temp).rotate(poleRotation), pos);
        }
    }

    private static @NotNull Vector3f projectOntoPlane(@NotNull Vector3f root, @NotNull Vector3f target, @NotNull Vector3f chain, @NotNull Vector3f dest) {
        var dx = target.x - root.x;
        var dy = target.y - root.y;
        var dz = target.z - root.z;
        var dot = -fma(dx, chain.x, fma(dy, chain.y, dz * chain.z));
        return dest.set(
            fma(chain.x, dot, dx),
            fma(chain.y, dot, dy),
            fma(chain.z, dot, dz)
        );
    }
}
