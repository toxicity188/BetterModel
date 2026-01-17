/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.platform;

import kr.toxicity.model.api.platform.PlatformLivingEntity;
import kr.toxicity.model.api.platform.PlatformLocation;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class BukkitLivingEntity extends BukkitEntity implements PlatformLivingEntity {

    public BukkitLivingEntity(@NotNull LivingEntity source) {
        super(source);
    }

    @Override
    public LivingEntity source() {
        return (LivingEntity) super.source();
    }

    @Override
    public @NotNull PlatformLocation eyeLocation() {
        return BukkitAdapter.adapt(source().getEyeLocation());
    }
}
