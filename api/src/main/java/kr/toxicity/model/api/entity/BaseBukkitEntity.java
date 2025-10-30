/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.util.TransformedItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An adapter of bukkit entity
 */
public interface BaseBukkitEntity extends BaseEntity {

    /**
     * Gets source entity
     * @return entity
     */
    @NotNull Entity entity();

    @Override
    default @Nullable TransformedItemStack mainHand() {
        if (entity() instanceof LivingEntity livingEntity) {
            var equipment = livingEntity.getEquipment();
            if (equipment != null) return TransformedItemStack.of(equipment.getItemInMainHand());
        }
        return TransformedItemStack.empty();
    }

    @Override
    default @Nullable TransformedItemStack offHand() {
        if (entity() instanceof LivingEntity livingEntity) {
            var equipment = livingEntity.getEquipment();
            if (equipment != null) return TransformedItemStack.of(equipment.getItemInOffHand());
        }
        return TransformedItemStack.empty();
    }
}
