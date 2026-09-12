/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.chat

import kr.toxicity.model.api.mod.BetterModelMod
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences
import net.minecraft.network.chat.Component

fun net.kyori.adventure.text.Component.asVanilla(): Component = MinecraftServerAudiences.of(BetterModelMod.platform().server()).asNative(this)
fun Component.asAdventure(): net.kyori.adventure.text.Component = MinecraftServerAudiences.of(BetterModelMod.platform().server()).asAdventure(this)
