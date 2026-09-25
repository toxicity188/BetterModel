/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains events emitted by model hit boxes.
 * <p>
 * Hit box events cover creation, removal, interaction, damage, mount, and
 * dismount notifications for the low-level hit box contracts exposed by the API.
 * They are separated from generic model events so listeners can target collision
 * and interaction behavior directly.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BetterModel.eventBus().subscribe(application, HitBoxInteractAtEvent.class, event -> {
 *     var hitBox = event.hitBox();
 * });
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.event.hitbox;
