package kr.toxicity.model.nms.v1_21_R5

import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener

internal typealias VanillaItemStack = net.minecraft.world.item.ItemStack
internal typealias BukkitItemStack = org.bukkit.inventory.ItemStack
internal typealias ClientPacket = Packet<ClientGamePacketListener>
internal typealias VanillaComponent = net.minecraft.network.chat.Component
internal typealias AdventureComponent = net.kyori.adventure.text.Component