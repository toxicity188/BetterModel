package kr.toxicity.model.nms.v1_21_R4

import kr.toxicity.library.sharedpackets.PluginBundlePacket
import kr.toxicity.model.api.nms.PacketBundler
import net.kyori.adventure.key.Key
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

internal class PacketBundlerImpl(
    private val list: MutableList<Packet<ClientGamePacketListener>>
) : PacketBundler, PluginBundlePacket<Packet<ClientGamePacketListener>> by PluginBundlePacket.of(key, list) {
    private companion object {
        private val key = Key.key("bettermodel")
    }
    private val bundlePacket = ClientboundBundlePacket(this)
    override fun copy(): PacketBundler = PacketBundlerImpl(ArrayList(list))
    override fun send(player: Player, onSuccess: Runnable) {
        val connection = (player as CraftPlayer).handle.connection
        if (list.isNotEmpty()) connection.send(bundlePacket, PacketSendListener.thenRun(onSuccess))
    }
    override fun isEmpty(): Boolean = list.isEmpty()
    operator fun plusAssign(other: Packet<ClientGamePacketListener>) {
        list += other
    }
}