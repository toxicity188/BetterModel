/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.mod;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.mod.scheduler.ModModelScheduler;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the Mod-specific platform interface for BetterModel.
 * <p>
 * This interface extends {@link BetterModelPlatform} to provide access to the underlying
 * Minecraft server instance and region holder for thread-safe operations.
 * </p>
 *
 * @since 2.0.0
 */
public interface BetterModelMod extends BetterModelPlatform {

    /**
     * Returns the current {@link BetterModelMod} instance.
     *
     * @return the current platform instance
     * @since 2.0.0
     */
    static @NotNull BetterModelMod platform() {
        return (BetterModelMod) BetterModel.platform();
    }

    /**
     * Returns the underlying Minecraft server instance.
     *
     * @return the Minecraft server
     * @since 2.0.0
     */
    @NotNull MinecraftServer server();

    /**
     * Returns the Mod-specific scheduler.
     *
     * @return the scheduler
     * @since 2.0.0
     */
    @Override
    @NotNull ModModelScheduler scheduler();
}
