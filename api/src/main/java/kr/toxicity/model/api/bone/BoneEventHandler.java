/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;

public interface BoneEventHandler {
    @NotNull BoneEventDispatcher eventDispatcher();

    default void extend(@NotNull BoneEventHandler eventHandler) {
        eventDispatcher().extend(eventHandler.eventDispatcher());
    }
}
