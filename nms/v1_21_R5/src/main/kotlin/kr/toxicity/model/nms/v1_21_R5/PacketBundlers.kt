package kr.toxicity.model.nms.v1_21_R5

import kr.toxicity.library.sharedpackets.PluginBundlePacket
import kr.toxicity.model.api.nms.PacketBundler
import net.kyori.adventure.key.Key
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

private val KEY = Key.key("bettermodel")

internal fun bundlerOfNotNull(vararg packets: ClientPacket?) = SimpleBundler(if (packets.isEmpty()) arrayListOf() else packets.filterNotNull().toMutableList())
internal fun bundlerOf(vararg packets: ClientPacket) = SimpleBundler(if (packets.isEmpty()) arrayListOf() else packets.toMutableList())
internal fun bundlerOf(size: Int) = SimpleBundler(ArrayList(size))
internal fun lazyBundlerOf() = LazyBundler()
internal fun parallelBundlerOf(threshold: Int) = ParallelBundler(threshold)
private fun uoe() = UnsupportedOperationException("cannot be added after PacketBundler#send is called.")
internal operator fun PacketBundler.plusAssign(other: ClientPacket) {
    when (this) {
        is SimpleBundler -> add(other)
        is LazyBundler -> add(other)
        is ParallelBundler -> add(other)
        else -> throw RuntimeException("unsupported bundler.")
    }
}
internal fun Packet<*>.assumeSize() = when (this) {
    is ClientboundSetEntityDataPacket -> packedItems.size
    is ClientboundSetEquipmentPacket -> slots.size
    else -> 1
}

internal interface PluginBundlePacketImpl : PluginBundlePacket<ClientPacket> {
    val bundlePacket: ClientboundBundlePacket
    fun size(): Int
    fun isEmpty(): Boolean
    fun add(other: ClientPacket)
}

internal class SimpleBundler(
    private val list: MutableList<ClientPacket>
) : PacketBundler, PluginBundlePacketImpl {
    override val bundlePacket = ClientboundBundlePacket(this)
    override fun send(player: Player, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = (player as CraftPlayer).handle.connection
        connection.send(bundlePacket, PacketSendListener.thenRun(onSuccess))
    }
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun size(): Int = list.size
    override fun key(): Key = KEY
    override fun iterator(): MutableIterator<ClientPacket> = list.iterator()
    override fun add(other: ClientPacket) {
        list += other
    }
}

internal class LazyBundler : PacketBundler, PluginBundlePacketImpl {
    private var index = 0
    private var listBuilder: (MutableList<ClientPacket>) -> Unit = {}
    private val list by lazy {
        sent = true
        ArrayList<ClientPacket>(index).also(listBuilder)
    }
    private var sent = false

    override val bundlePacket = ClientboundBundlePacket(this)
    override fun send(player: Player, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = (player as CraftPlayer).handle.connection
        connection.send(bundlePacket, PacketSendListener.thenRun(onSuccess))
    }
    override fun isEmpty(): Boolean = size() == 0
    override fun size(): Int = index
    override fun key(): Key = KEY
    override fun iterator(): MutableIterator<ClientPacket> = list.iterator()
    override fun add(other: ClientPacket) {
        if (sent) throw uoe()
        if (index++ == 0) {
            listBuilder = { it += other }
            return
        }
        val previous = listBuilder
        listBuilder = {
            previous(it)
            it += other
        }
    }
}

internal class ParallelBundler(
    private val threshold: Int
) : PacketBundler {
    private val _creator: () -> PluginBundlePacketImpl = if (threshold < 32) { { lazyBundlerOf() } } else { { bundlerOf() } }
    private val subBundlers = mutableListOf<PluginBundlePacketImpl>()
    private var sizeAssume = 0
    private val newBundler get() = _creator().apply {
        sizeAssume = 0
        subBundlers += this
    }
    private var selectedBundler = newBundler
    override fun send(player: Player, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = (player as CraftPlayer).handle.connection
        subBundlers.forEach {
            connection.send(it.bundlePacket)
        }
    }
    override fun isEmpty(): Boolean = selectedBundler.isEmpty()
    override fun size(): Int = subBundlers.sumOf(PluginBundlePacketImpl::size)
    fun add(other: ClientPacket) {
        (if (sizeAssume > threshold) newBundler else selectedBundler)
            .apply { selectedBundler = this }
            .add(other)
        sizeAssume += other.assumeSize()
    }
}