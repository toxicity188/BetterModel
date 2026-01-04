/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_21_R5;

import kr.toxicity.model.api.nms.HitBox;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractHitBox extends ArmorStand implements HitBox {

    AbstractHitBox(@NotNull Level level) {
        super(EntityType.ARMOR_STAND, level);
    }

    @Override //Only for provide compiler hint for Kotlin jvm
    public final boolean equals(@Nullable Object other) {
        return super.equals(other);
    }

    @Override //Only for provide compiler hint for Kotlin jvm
    public final int hashCode() {
        return super.hashCode();
    }
}
