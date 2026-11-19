/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Describes Minecraft version information known to the API.
 * <p>
 * Version contracts are used by platform and NMS selection code to reason about
 * supported Minecraft releases while keeping version identifiers centralized.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * MinecraftVersion version = MinecraftVersion.parse("1.21.11");
 * boolean newer = version.compareTo(MinecraftVersion.V1_21_10) > 0;
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.version;
