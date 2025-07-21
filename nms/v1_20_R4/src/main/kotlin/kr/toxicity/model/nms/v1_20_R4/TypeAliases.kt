package kr.toxicity.model.nms.v1_20_R4

import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.world.item.ItemStack

internal typealias VanillaItemStack = ItemStack
internal typealias BukkitItemStack = org.bukkit.inventory.ItemStack
internal typealias ClientPacket = Packet<ClientGamePacketListener>