/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;

public interface PlatformLivingEntity extends PlatformEntity {

    @NotNull PlatformLocation eyeLocation();
}
