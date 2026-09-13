/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines BetterModel lifecycle, tracker, animation, skin, and asset events.
 * <p>
 * Event contracts in this package are dispatched through the API event bus and
 * describe observable model behavior such as reloads, imports, tracker creation,
 * player visibility, mounting, and animation signals. Platform adapters may wrap
 * these events, but the event payloads remain platform-neutral API contracts.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * BetterModel.eventBus().subscribe(application, ModelImportedEvent.class, event -> {
 *     var renderer = event.renderer();
 * });
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.event;
