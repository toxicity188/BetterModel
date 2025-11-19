/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.command

import kr.toxicity.model.util.PLUGIN
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.description.Description
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager

class CommandBuildContext(
    name: String,
    description: String,
    vararg aliases: String,
) {
    private val manager = LegacyPaperCommandManager.createNative(
        PLUGIN,
        ExecutionCoordinator.simpleCoordinator(),
    ).apply {
        if (hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            registerBrigadier()
            brigadierManager().setNativeNumberSuggestions(true)
        } else if (hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) registerAsynchronousCompletions()
    }
    val root = CommandBuilder(
        null,
        manager,
        CommandBuilder.Info(name, Description.description(description), aliases.toList())
    )

    fun build() {
        root.build().forEach {
            manager.command(it)
        }
    }
}