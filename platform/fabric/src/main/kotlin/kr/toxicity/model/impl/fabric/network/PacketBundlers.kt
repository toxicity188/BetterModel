/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.impl.fabric.network

import kr.toxicity.model.api.nms.PacketBundler
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.impl.fabric.unwarp
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket

internal typealias ClientPacket = Packet<ClientGamePacketListener>

internal fun bundlerOfNotNull(vararg packets: ClientPacket?) = SimpleBundler(if (packets.isEmpty()) arrayListOf() else packets.filterNotNull().toMutableList())
internal fun bundlerOf(vararg packets: ClientPacket) = SimpleBundler(if (packets.isEmpty()) arrayListOf() else packets.toMutableList())
internal fun bundlerOf(size: Int) = SimpleBundler(ArrayList(size))
internal fun parallelBundlerOf(threshold: Int) = ParallelBundler(threshold)

internal operator fun PacketBundler.plusAssign(other: ClientPacket) {
    when (this) {
        is SimpleBundler -> add(other)
        is ParallelBundler -> add(other)
        else -> throw RuntimeException("unsupported bundler.")
    }
}
internal fun Packet<*>.assumeSize() = when (this) {
    is ClientboundSetEntityDataPacket -> packedItems.size
    is ClientboundSetEquipmentPacket -> slots.size
    else -> 1
}

internal interface PluginBundlePacketImpl : Iterable<ClientPacket> {
    val bundlePacket: ClientboundBundlePacket
    fun size(): Int
    fun isEmpty(): Boolean
    fun add(other: ClientPacket)
}

internal class SimpleBundler(
    private val list: MutableList<ClientPacket>
) : PacketBundler, PluginBundlePacketImpl {
    override val bundlePacket by lazy {
        ClientboundBundlePacket(this).apply {
            (this as BetterModelBundlePacket).`bettermodel$setBetterModelPacket`(true)
        }
    }
    override fun send(player: PlatformPlayer, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = player.unwarp().player.connection
        connection.send(bundlePacket, PacketSendListener.thenRun(onSuccess))
    }
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun size(): Int = list.size
    override fun iterator(): MutableIterator<ClientPacket> = list.iterator()
    override fun add(other: ClientPacket) {
        list += other
    }
}

internal class ParallelBundler(
    private val threshold: Int
) : PacketBundler {
    private val subBundlers = mutableListOf<PluginBundlePacketImpl>()
    private var sizeAssume = 0
    private val newBundler get() = bundlerOf().apply {
        sizeAssume = 0
        subBundlers += this
    }
    private var selectedBundler = newBundler
    override fun send(player: PlatformPlayer, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = player.unwarp()
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
