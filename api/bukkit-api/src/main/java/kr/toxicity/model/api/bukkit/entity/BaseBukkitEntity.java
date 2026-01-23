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
 * Represents a Bukkit-specific entity adapter.
 * <p>
 * This interface extends {@link BaseEntity} and {@link PersistentDataHolder} to provide
 * access to the underlying Bukkit entity and its persistent data container.
 * </p>
 *
 * @since 2.0.0
 */
public interface BaseBukkitEntity extends BaseEntity, PersistentDataHolder {

    /**
     * The namespaced key used for storing tracker data in the entity's persistent data container.
     * @since 2.0.0
     */
    @NotNull
    NamespacedKey TRACKING_ID = Objects.requireNonNull(NamespacedKey.fromString("bettermodel_tracker"));

    /**
     * Returns the underlying Bukkit entity.
     *
     * @return the Bukkit entity
     * @since 2.0.0
     */
    default @NotNull Entity entity() {
        return ((BukkitEntity) platform()).source();
    }

    /**
     * Returns the item in the entity's main hand.
     *
     * @return the main hand item
     * @since 2.0.0
     */
    @Override
    default @NotNull TransformedItemStack mainHand() {
        if (entity() instanceof LivingEntity livingEntity) {
            var equipment = livingEntity.getEquipment();
            if (equipment != null) return TransformedItemStack.of(BukkitAdapter.adapt(equipment.getItemInMainHand()));
        }
        return TransformedItemStack.empty();
    }

    /**
     * Returns the item in the entity's offhand.
     *
     * @return the offhand item
     * @since 2.0.0
     */
    @Override
    default @NotNull TransformedItemStack offHand() {
        if (entity() instanceof LivingEntity livingEntity) {
            var equipment = livingEntity.getEquipment();
            if (equipment != null) return TransformedItemStack.of(BukkitAdapter.adapt(equipment.getItemInOffHand()));
        }
        return TransformedItemStack.empty();
    }

    /**
     * Retrieves the model data stored in the entity's persistent data container.
     *
     * @return the model data string, or null if not present
     * @since 2.0.0
     */
    default @Nullable String modelData() {
        return getPersistentDataContainer().get(TRACKING_ID, PersistentDataType.STRING);
    }

    /**
     * Stores the model data in the entity's persistent data container.
     *
     * @param modelData the model data string, or null to remove it
     * @since 2.0.0
     */
    default void modelData(@Nullable String modelData) {
        var container = getPersistentDataContainer();
        if (modelData == null) container.remove(TRACKING_ID);
        else container.set(TRACKING_ID, PersistentDataType.STRING, modelData);
    }
}
