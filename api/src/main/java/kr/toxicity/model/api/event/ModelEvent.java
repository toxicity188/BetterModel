/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelEventBus;
import org.jetbrains.annotations.NotNull;

public interface ModelEvent {

    default @NotNull BetterModelEventBus.Result call() {
        return BetterModel.eventBus().call(this);
    }
}
