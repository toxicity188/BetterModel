/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit;

import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.bukkit.scheduler.BukkitModelScheduler;
import org.jetbrains.annotations.NotNull;

import static kr.toxicity.model.api.util.ReflectionUtil.classExists;

public interface BetterModelBukkit extends BetterModelPlatform {

    /**
     * Checks if the server is running on the Folia platform.
     * @since 1.15.2
     */
    boolean IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    /**
     * Checks if the server is running on the Purpur platform.
     * @since 1.15.2
     */
    boolean IS_PURPUR = classExists("org.purpurmc.purpur.PurpurConfig");
    /**
     * Checks if the server is running on the Paper platform (or a fork like Purpur/Folia).
     * @since 1.15.2
     */
    boolean IS_PAPER = IS_PURPUR || IS_FOLIA || classExists("io.papermc.paper.configuration.PaperConfigurations");

    @Override
    @NotNull BukkitModelScheduler scheduler();

    @Override
    @NotNull BukkitAdapter adapter();
}
