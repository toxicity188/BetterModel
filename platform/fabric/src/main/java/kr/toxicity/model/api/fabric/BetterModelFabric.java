/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.fabric.scheduler.FabricModelScheduler;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the Fabric-specific platform interface for BetterModel.
 * <p>
 * This interface extends {@link BetterModelPlatform} to provide access to the underlying
 * Minecraft server instance and region holder for thread-safe operations.
 * </p>
 *
 * @since 2.0.0
 */
public interface BetterModelFabric extends BetterModelPlatform {

    /**
     * Returns the current {@link BetterModelFabric} instance.
     *
     * @return the current platform instance
     * @since 2.0.0
     */
    static @NotNull BetterModelFabric platform() {
        return (BetterModelFabric) BetterModel.platform();
    }

    /**
     * Returns the underlying Minecraft server instance.
     *
     * @return the Minecraft server
     * @since 2.0.0
     */
    @NotNull MinecraftServer server();

    /**
     * Returns the Fabric-specific scheduler.
     *
     * @return the scheduler
     * @since 2.0.0
     */
    @Override
    @NotNull FabricModelScheduler scheduler();
}
