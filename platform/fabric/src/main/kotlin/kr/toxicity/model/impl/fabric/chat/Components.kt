/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.chat

import net.kyori.adventure.platform.modcommon.impl.NonWrappingComponentSerializer
import net.minecraft.network.chat.Component

fun net.kyori.adventure.text.Component.asVanilla(): Component = NonWrappingComponentSerializer.INSTANCE.serialize(this)
fun Component.asAdventure(): net.kyori.adventure.text.Component = NonWrappingComponentSerializer.INSTANCE.deserialize(this)
