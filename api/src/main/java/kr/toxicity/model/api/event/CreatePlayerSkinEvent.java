/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.profile.ModelProfile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Create player skin data event
 */
@Getter
@Setter
public final class CreatePlayerSkinEvent implements ModelEvent {

    private ModelProfile modelProfile;

    /**
     * Creates event
     * @param modelProfile model skin
     */
    @ApiStatus.Internal
    public CreatePlayerSkinEvent(@NotNull ModelProfile modelProfile) {
        this.modelProfile = modelProfile;
    }
}
