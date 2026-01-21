/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.impl.fabric.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractArmorStand extends ArmorStand {
    public AbstractArmorStand(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
    }

    @Override
    public final boolean equals(@Nullable Object object) {
        return super.equals(object);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}
