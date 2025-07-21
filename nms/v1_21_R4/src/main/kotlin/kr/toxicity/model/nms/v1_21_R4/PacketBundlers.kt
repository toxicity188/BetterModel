package kr.toxicity.model.nms.v1_21_R4

import kr.toxicity.library.sharedpackets.PluginBundlePacket
import kr.toxicity.model.api.nms.PacketBundler
import net.kyori.adventure.key.Key
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

private val KEY = Key.key("bettermodel")

internal fun bundlerOf(vararg packets: Packet<ClientGamePacketListener>) = SimpleBundler(if (packets.isEmpty()) arrayListOf() else packets.toMutableList())
internal fun bundlerOf(size: Int) = SimpleBundler(ArrayList(size))
internal fun parallelBundlerOf(threshold: Int) = ParallelBundler(threshold)
internal operator fun PacketBundler.plusAssign(other: Packet<ClientGamePacketListener>) {
    when (this) {
        is SimpleBundler -> add(other)
        is ParallelBundler -> add(other)
    }
}
internal fun Packet<*>.assumeSize() = when (this) {
    is ClientboundSetEntityDataPacket -> packedItems.size
    is ClientboundSetEquipmentPacket -> slots.size
    else -> 1
}

internal class SimpleBundler(
    private val list: MutableList<Packet<ClientGamePacketListener>>
) : PacketBundler, PluginBundlePacket<Packet<ClientGamePacketListener>> by PluginBundlePacket.of(KEY, list) {
    val bundlePacket = ClientboundBundlePacket(this)
    override fun send(player: Player, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = (player as CraftPlayer).handle.connection
        connection.send(bundlePacket, PacketSendListener.thenRun(onSuccess))
    }
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun size(): Int = list.size
    fun add(other: Packet<ClientGamePacketListener>) {
        list += other
    }
}

internal class ParallelBundler(val threshold: Int) : PacketBundler {
    private var sizeAssume = 0
    private val subBundlers = mutableListOf(bundlerOf())
    private var selectedBundler = subBundlers.first()
    override fun send(player: Player, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = (player as CraftPlayer).handle.connection
        subBundlers.forEach {
            connection.send(it.bundlePacket)
        }
    }
    override fun isEmpty(): Boolean = selectedBundler.isEmpty()
    override fun size(): Int = subBundlers.sumOf(SimpleBundler::size)
    fun add(other: Packet<ClientGamePacketListener>) {
        sizeAssume += other.assumeSize()
        val bundler = if (sizeAssume > threshold) {
            sizeAssume = 0
            bundlerOf().apply {
                subBundlers += this
                selectedBundler = this
            }
        } else selectedBundler
        bundler.add(other)
    }
}