/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;

public interface PlatformLocation extends PlatformRegionHolder {

    PlatformWorld world();

    double x();
    double y();
    double z();

    float pitch();
    float yaw();

    @NotNull PlatformLocation add(double x, double y, double z);
}
