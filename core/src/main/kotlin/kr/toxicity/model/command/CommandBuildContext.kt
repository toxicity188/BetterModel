/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.command

import net.kyori.adventure.audience.Audience
import org.incendo.cloud.Command
import org.incendo.cloud.CommandManager
import org.incendo.cloud.description.Description

class CommandBuildContext(
    val manager: CommandManager<Audience>,
    val commandMapper: CommandBuilder.(Command.Builder<Audience>) -> Command.Builder<Audience>,
    name: String,
    description: String,
    vararg aliases: String,
) {
    val root = CommandBuilder(
        null,
        this,
        CommandBuilder.Info(name, Description.description(description), aliases.toList())
    )

    fun build() {
        root.build().forEach {
            manager.command(it)
        }
    }
}
