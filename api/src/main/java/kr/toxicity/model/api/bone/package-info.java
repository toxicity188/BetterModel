/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides contracts for rendered model bones and bone-level behavior.
 * <p>
 * This package includes bone names, positions, movement snapshots, render
 * context, tag registration, item mapping, inverse kinematics helpers, and
 * event dispatching. These types describe how a model part is addressed and
 * manipulated while leaving packet and platform details outside the API layer.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BoneName name = BoneName.of("h_head");
 * boolean followsHead = name.tagged(BoneTags.HEAD);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.bone;
