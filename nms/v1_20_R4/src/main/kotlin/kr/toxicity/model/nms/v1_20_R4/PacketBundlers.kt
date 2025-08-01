package kr.toxicity.model.nms.v1_20_R4

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

internal class SimpleBundler(
    private val list: MutableList<ClientPacket>
) : PacketBundler, PluginBundlePacket<ClientPacket> by PluginBundlePacket.of(KEY, list) {
    val bundlePacket = ClientboundBundlePacket(this)
    override fun send(player: Player, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = (player as CraftPlayer).handle.connection
        connection.send(bundlePacket, PacketSendListener.thenRun(onSuccess))
    }
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun size(): Int = list.size
    fun add(other: ClientPacket) {
        list += other
    }
}

internal class LazyBundler : PacketBundler, PluginBundlePacket<ClientPacket> {
    private var index = 0
    private var listBuilder: (MutableList<ClientPacket>) -> Unit = {}
    private val list by lazy {
        sent = true
        ArrayList<ClientPacket>(index).also(listBuilder)
    }
    private var sent = false

    val bundlePacket = ClientboundBundlePacket(this)
    override fun send(player: Player, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = (player as CraftPlayer).handle.connection
        connection.send(bundlePacket, PacketSendListener.thenRun(onSuccess))
    }
    override fun isEmpty(): Boolean = size() == 0
    override fun size(): Int = index
    fun add(other: ClientPacket) {
        if (sent) throw uoe()
        val previous = listBuilder
        listBuilder = {
            previous(it)
            it += other
        }
        index++
    }

    override fun key(): Key = KEY
    override fun iterator(): MutableIterator<ClientPacket> = list.iterator()
}

internal class ParallelBundler(private val threshold: Int) : PacketBundler {
    private var sizeAssume = 0
    private var selectedBundler = lazyBundlerOf()
    private var listBuilder: (MutableList<LazyBundler>) -> Unit = selectedBundler.run {
        {
            it += this
        }
    }
    private var index = 1
    private var size = 0
    private val subBundlers by lazy {
        sent = true
        ArrayList<LazyBundler>(index).also(listBuilder)
    }
    private var sent = false
    override fun send(player: Player, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = (player as CraftPlayer).handle.connection
        subBundlers.forEach {
            connection.send(it.bundlePacket)
        }
    }
    override fun isEmpty(): Boolean = size() == 0
    override fun size(): Int = size
    fun add(other: ClientPacket) {
        if (sent) throw uoe()
        sizeAssume += other.assumeSize()
        val bundler = if (sizeAssume > threshold) {
            val previous = listBuilder
            lazyBundlerOf().apply {
                listBuilder = {
                    previous(it)
                    it += this
                }
                selectedBundler = this
                sizeAssume = 0
                index++
            }
        } else selectedBundler
        bundler.add(other)
        size++
    }
}