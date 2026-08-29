/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines low-level Minecraft server adapter contracts used by BetterModel.
 * <p>
 * This package is the API boundary for version-specific NMS implementations,
 * including display entities, hit boxes, packet bundling, player channels,
 * nametags, and version selection. Implementations belong in NMS modules; API
 * consumers should treat these types as contracts for packet-based rendering.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * NMS nms = BetterModel.nms();
 * PlayerChannelHandler channel = BetterModel.player(player.uniqueId()).orElseThrow();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.nms;
