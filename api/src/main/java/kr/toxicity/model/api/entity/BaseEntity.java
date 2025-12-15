/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.nms.Identifiable;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import kr.toxicity.model.api.util.TransformedItemStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * An adapter of entity
 */
public interface BaseEntity extends Identifiable, PersistentDataHolder {

    /**
     * Gets base entity
     * @param entity bukkit entity
     * @return base entity
     */
    static @NotNull BaseBukkitEntity of(@NotNull Entity entity) {
        if (entity instanceof Player player) {
            var channel = BetterModel.plugin().playerManager().player(player.getUniqueId());
            if (channel != null) return channel.base();
        }
        return BetterModel.nms().adapt(entity);
    }

    /**
     * Gets custom name of this entity
     * @return custom name
     */
    @Nullable Component customName();

    /**
     * Gets vanilla entity
     * @return vanilla entity
     */
    @NotNull Object handle();

    /**
     * Gets entity id
     * @return entity id
     */
    int id();

    /**
     * Checks source entity is dead
     * @return dead
     */
    boolean dead();

    /**
     * Checks source entity is on the ground
     * @return on the ground
     */
    boolean ground();

    /**
     * Checks source entity is invisible
     * @return invisible
     */
    boolean invisible();

    /**
     * Check source entity is on a glow
     * @return glow
     */
    boolean glow();

    /**
     * Check source entity is on a walk
     * @return walk
     */
    boolean onWalk();

    /**
     * Check source entity is on the fly
     * @return fly
     */
    boolean fly();

    /**
     * Gets entity's scale
     * @return scale
     */
    double scale();

    /**
     * Gets entity's pitch (x-rot)
     * @return pitch
     */
    float pitch();

    /**
     * Gets entity's body yaw (y-rot)
     * @return body yaw
     */
    float bodyYaw();

    /**
     * Gets entity's yaw (y-rot)
     * @return yaw
     */
    float headYaw();

    /**
     * Gets entity's damage tick
     * @return damage tick
     */
    float damageTick();

    /**
     * Gets entity's walk speed
     * @return walk speed
     */
    float walkSpeed();

    /**
     * Gets entity's passenger point
     * @param dest destination vector
     * @return passenger point
     */
    @NotNull Vector3f passengerPosition(@NotNull Vector3f dest);

    /**
     * Gets tracked player set
     * @return tracked player set
     */
    @NotNull Stream<Player> trackedBy();

    /**
     * Gets location
     * @return location
     */
    @NotNull Location location();

    /**
     * Gets main hand item
     * @return main hand
     */
    @NotNull TransformedItemStack mainHand();

    /**
     * Gets offhand item
     * @return offhand
     */
    @NotNull TransformedItemStack offHand();

    /**
     * Gets tracker registry of this adapter
     * @return optional tracker registry
     */
    default @NotNull Optional<EntityTrackerRegistry> registry() {
        return BetterModel.registry(uuid());
    }

    /**
     * Checks this entity has controlling passenger
     * @return has controlling passenger
     */
    default boolean hasControllingPassenger() {
        var registry = registry().orElse(null);
        return registry != null && registry.hasControllingPassenger();
    }

    /**
     * Checks this entity has model data
     * @return has model data
     */
    default boolean hasModelData() {
        return modelData() != null;
    }

    /**
     * Gets this entity's model data
     * @return model data
     */
    default @Nullable String modelData() {
        return getPersistentDataContainer().get(EntityTrackerRegistry.TRACKING_ID, PersistentDataType.STRING);
    }

    /**
     * Sets this entity's model data
     * @param modelData model data
     */
    default void modelData(@Nullable String modelData) {
        var container = getPersistentDataContainer();
        if (modelData == null) container.remove(EntityTrackerRegistry.TRACKING_ID);
        else container.set(EntityTrackerRegistry.TRACKING_ID, PersistentDataType.STRING, modelData);
    }
}
