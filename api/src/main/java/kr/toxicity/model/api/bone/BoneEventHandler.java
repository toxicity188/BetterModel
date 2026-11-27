/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;

/**
 * Exposes the dispatcher used to register and forward bone events.
 */
public interface BoneEventHandler {
    /**
     * Returns the event dispatcher for this handler.
     *
     * @return the dispatcher used by this handler
     */
    @NotNull BoneEventDispatcher eventDispatcher();

    /**
     * Adds the other handler’s dispatcher to this handler.
     *
     * @param eventHandler supplies the dispatcher to attach
     */
    default void extend(@NotNull BoneEventHandler eventHandler) {
        eventDispatcher().extend(eventHandler.eventDispatcher());
    }
}
