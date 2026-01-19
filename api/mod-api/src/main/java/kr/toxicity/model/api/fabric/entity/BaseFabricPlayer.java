/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric.entity;

import kr.toxicity.model.api.entity.BasePlayer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Fabric-specific player adapter.
 * <p>
 * This interface extends {@link BaseFabricEntity} and {@link BasePlayer} to provide
 * access to the underlying NMS server player.
 * </p>
 *
 * @since 2.0.0
 */
public interface BaseFabricPlayer extends BaseFabricEntity, BasePlayer {

    /**
     * Returns the underlying NMS server player.
     *
     * @return the server player
     * @since 2.0.0
     */
    @Override
    default @NotNull ServerPlayer entity() {
        return (ServerPlayer) handle();
    }
}
