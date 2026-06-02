/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains mod-backed entity wrappers for BetterModel.
 * <p>
 * The wrappers in this package adapt Minecraft mod runtime entities and players
 * to the platform-neutral entity contracts from the base API module. They allow
 * renderer and tracker code to stay generic while mod integrations provide the
 * concrete entity access.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BaseModPlayer player = ...;
 * var minecraftPlayer = player.player();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.mod.entity;
