/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.tracker;

import lombok.RequiredArgsConstructor;

/**
 * Reason for closing a tracker.
 * <p>
 * Represents the cause of a tracker closure and indicates whether the tracker state
 * should be persisted.
 * </p>
 * <p>Example:</p>
 * <pre>{@code
 * tracker.handleCloseEvent((t, reason) -> {
 *     if (reason.shouldBeSave()) {
 *         // persist state
 *     }
 * });
 * }</pre>
 *
 * @since 3.7.0
 */
@RequiredArgsConstructor
public enum TrackerCloseReason {
    /**
     * The tracker was manually removed.
     * @since 3.7.0
     */
    REMOVE(false),
    /**
     * The plugin is being disabled.
     * @since 3.7.0
     */
    PLUGIN_DISABLE(true),
    /**
     * The entity or tracker was despawned.
     * @since 3.7.0
     */
    DESPAWN(true)
    ;
    private final boolean save;

    /**
     * Checks if the tracker state should be saved.
     *
     * @return true if the tracker state should be saved, false otherwise
     * @since 3.7.0
     */
    public boolean shouldBeSave() {
        return save;
    }
}
