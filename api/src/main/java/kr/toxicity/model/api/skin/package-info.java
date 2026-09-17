/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains player skin data used by rendered model bones.
 * <p>
 * Skin data describes the resolved texture and appearance information needed by
 * player skin renderers. It is an API data contract and does not perform profile
 * lookup or platform-specific texture application.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * SkinData skin = context.skin();
 * ModelProfile profile = skin.profile();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.skin;
