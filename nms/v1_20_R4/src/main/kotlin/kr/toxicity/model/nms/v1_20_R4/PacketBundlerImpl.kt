package kr.toxicity.model.nms.v1_20_R4

import kr.toxicity.model.api.nms.PacketBundler
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

internal class PacketBundlerImpl(
    private val list: MutableList<Packet<ClientGamePacketListener>>
) : PacketBundler, Iterable<Packet<ClientGamePacketListener>> by list {
    private val bundlePacket by lazy {
        ClientboundBundlePacket(this)
    }
    override fun copy(): PacketBundler = PacketBundlerImpl(ArrayList(list))
    override fun send(player: Player, onSuccess: Runnable) {
        val connection = (player as CraftPlayer).handle.connection
        when (list.size) {
            0 -> {}
            1 -> connection.send(list[0], PacketSendListener.thenRun(onSuccess))
            else -> connection.send(bundlePacket, PacketSendListener.thenRun(onSuccess))
        }
    }
    override fun isEmpty(): Boolean = list.isEmpty()
    operator fun plusAssign(other: Packet<ClientGamePacketListener>) {
        list += other
    }
}