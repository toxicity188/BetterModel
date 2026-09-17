/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Provides player model metadata contracts.
 * <p>
 * This package describes player limbs and skin-part visibility used by player
 * skin rendering. It keeps player appearance state separate from any particular
 * platform player implementation.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * PlayerSkinParts parts = PlayerSkinParts.DEFAULT;
 * boolean jacketVisible = parts.isJacketEnabled();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.player;
