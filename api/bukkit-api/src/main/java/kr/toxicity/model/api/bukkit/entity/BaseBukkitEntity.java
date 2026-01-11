/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.entity;

import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.bukkit.platform.BukkitEntity;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.util.TransformedItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * An adapter of bukkit entity
 */
public interface BaseBukkitEntity extends BaseEntity, PersistentDataHolder {


    /**
     * The namespaced key used for tracking ID.
     * @since 1.15.2
     */
    @NotNull
    NamespacedKey TRACKING_ID = Objects.requireNonNull(NamespacedKey.fromString("bettermodel_tracker"));

    /**
     * Gets source entity
     * @return entity
     */
    default @NotNull Entity entity() {
        return ((BukkitEntity) platform()).source();
    }

    @Override
    default @NotNull TransformedItemStack mainHand() {
        if (entity() instanceof LivingEntity livingEntity) {
            var equipment = livingEntity.getEquipment();
            if (equipment != null) return TransformedItemStack.of(BukkitAdapter.adapt(equipment.getItemInMainHand()));
        }
        return TransformedItemStack.empty();
    }

    @Override
    default @NotNull TransformedItemStack offHand() {
        if (entity() instanceof LivingEntity livingEntity) {
            var equipment = livingEntity.getEquipment();
            if (equipment != null) return TransformedItemStack.of(BukkitAdapter.adapt(equipment.getItemInOffHand()));
        }
        return TransformedItemStack.empty();
    }

    /**
     * Gets this entity's model data
     * @return model data
     */
    default @Nullable String modelData() {
        return getPersistentDataContainer().get(TRACKING_ID, PersistentDataType.STRING);
    }

    /**
     * Sets this entity's model data
     * @param modelData model data
     */
    default void modelData(@Nullable String modelData) {
        var container = getPersistentDataContainer();
        if (modelData == null) container.remove(TRACKING_ID);
        else container.set(TRACKING_ID, PersistentDataType.STRING, modelData);
    }
}
