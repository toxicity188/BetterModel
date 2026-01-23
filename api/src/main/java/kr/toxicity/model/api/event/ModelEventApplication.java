/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

/**
 * Represents an application or plugin that subscribes to model events.
 * <p>
 * This interface is used to check if the subscribing application is still enabled,
 * allowing the event bus to automatically unregister listeners from disabled plugins.
 * </p>
 *
 * @since 2.0.0
 */
public interface ModelEventApplication {
    /**
     * Checks if the application is currently enabled.
     *
     * @return true if enabled, false otherwise
     * @since 2.0.0
     */
    boolean isEnabled();
}
