/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit.nms.v1_21_R7

import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.world.item.ItemStack

internal typealias VanillaItemStack = ItemStack
internal typealias BukkitItemStack = org.bukkit.inventory.ItemStack
internal typealias ClientPacket = Packet<ClientGamePacketListener>
internal typealias VanillaComponent = Component
internal typealias AdventureComponent = net.kyori.adventure.text.Component
