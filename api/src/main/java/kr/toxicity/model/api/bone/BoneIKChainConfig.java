/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Configuration for an inverse kinematics (IK) chain.
 *
 * @param ikTarget               the UUID of the end-effector (target) bone in the chain.
 * @param ikSource               nullable UUID of the root bone of the IK chain. If null, the root bone of the target will be used.
 * @param ikPole                 nullable UUID of the pole bone used to control the bending direction of the chain.
 * @param lockIkTargetRotation   whether to lock the target bone's original rotation instead of aligning it with the IK chain's solved orientation.
 * @since 3.6.0
 */
public record BoneIKChainConfig(
    @NotNull UUID ikTarget,
    @Nullable UUID ikSource,
    @Nullable UUID ikPole,
    boolean lockIkTargetRotation
) {
}
