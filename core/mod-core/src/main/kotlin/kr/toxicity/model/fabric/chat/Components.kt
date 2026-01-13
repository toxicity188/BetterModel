/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.chat

import com.google.gson.GsonBuilder
import com.mojang.serialization.JsonOps
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization

fun net.kyori.adventure.text.Component.asVanilla(): Component = GSON.fromJson(
    GsonComponentSerializer.gson().serialize(this),
    Component::class.java
)

fun Component.asAdventure(): net.kyori.adventure.text.Component = GsonComponentSerializer.gson().deserialize(
    GSON.toJson(
        ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow()
    )
)

private val GSON = GsonBuilder().disableHtmlEscaping().create()
