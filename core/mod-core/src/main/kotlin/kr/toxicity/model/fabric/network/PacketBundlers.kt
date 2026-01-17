/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.network

import kr.toxicity.model.api.nms.PacketBundler
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.fabric.modId
import kr.toxicity.model.fabric.unwarp
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Keyed
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket

fun parallelBundlerOf(threshold: Int): ParallelBundler = ParallelBundler(threshold)

fun bundlerOfNotNull(vararg packets: Packet<ClientGamePacketListener>?): SimpleBundler {
    return SimpleBundler(
        if (packets.isEmpty()) {
            arrayListOf()
        } else {
            packets.filterNotNull().toMutableList()
        }
    )
}

fun bundlerOf(vararg packets: Packet<ClientGamePacketListener>): SimpleBundler {
    return SimpleBundler(
        if (packets.isEmpty()) {
            arrayListOf()
        } else {
            packets.toMutableList()
        }
    )
}

fun bundlerOf(size: Int): SimpleBundler = SimpleBundler(ArrayList(size))

fun lazyBundlerOf(): LazyBundler = LazyBundler()

fun Packet<*>.assumeSize() = when (this) {
    is ClientboundSetEntityDataPacket -> {
        packedItems.size
    }

    is ClientboundSetEquipmentPacket -> {
        slots.size
    }

    else -> 1
}

operator fun PacketBundler.plusAssign(other: Packet<ClientGamePacketListener>) {
    when (this) {
        is SimpleBundler, is LazyBundler -> {
            add(other)
        }

        is ParallelBundler -> {
            add(other)
        }

        else -> throw RuntimeException("Unsupported bundler.")
    }
}

interface FabricBundlePacketImpl :
    Iterable<Packet<ClientGamePacketListener>>,
    Keyed
{
    val bundlePacket: ClientboundBundlePacket

    fun size(): Int

    fun isEmpty(): Boolean

    fun add(other: Packet<ClientGamePacketListener>)
}

class SimpleBundler internal constructor(
    private val packets: MutableList<Packet<ClientGamePacketListener>>
) :
    PacketBundler,
    FabricBundlePacketImpl
{
    override val bundlePacket = ClientboundBundlePacket(this)

    override fun send(player: PlatformPlayer, onSuccess: Runnable) {
        if (isEmpty) {
            return
        }

        player.unwarp().connection.send(
            bundlePacket,
            PacketSendListener.thenRun(onSuccess)
        )
    }

    override fun isEmpty(): Boolean = packets.isEmpty()

    override fun size(): Int = packets.size

    override fun key(): Key = KEY

    override fun iterator(): MutableIterator<Packet<ClientGamePacketListener>> = packets.iterator()

    override fun add(other: Packet<ClientGamePacketListener>) {
        packets += other
    }
}

class LazyBundler internal constructor() :
    PacketBundler,
    FabricBundlePacketImpl
{
    private var packetBuilder: (MutableList<Packet<ClientGamePacketListener>>) -> Unit = {}

    private var index = 0
    private var sent = false

    private val packets by lazy {
        sent = true
        ArrayList<Packet<ClientGamePacketListener>>(index).also {
            packetBuilder(it)
        }
    }

    override val bundlePacket = ClientboundBundlePacket(this)

    override fun send(player: PlatformPlayer, onSuccess: Runnable) {
        if (isEmpty) {
            return
        }

        player.unwarp().connection.send(
            bundlePacket,
            PacketSendListener.thenRun(onSuccess)
        )
    }

    override fun isEmpty(): Boolean = size() == 0

    override fun size(): Int = index

    override fun key(): Key = KEY

    override fun iterator(): MutableIterator<Packet<ClientGamePacketListener>> = packets.iterator()

    override fun add(other: Packet<ClientGamePacketListener>) {
        if (sent) {
            throw UnsupportedOperationException("cannot be added after PacketBundler#send is called.")
        }

        if (index++ == 0) {
            packetBuilder = { it += other }
            return
        }

        val previous = packetBuilder
        packetBuilder = {
            previous(it)
            it += other
        }
    }
}

class ParallelBundler internal constructor(
    private val threshold: Int
) : PacketBundler {
    private val _creator: () -> FabricBundlePacketImpl = if (threshold < 32) {
        { lazyBundlerOf() }
    } else {
        { bundlerOf() }
    }

    private val subBundlers = mutableListOf<FabricBundlePacketImpl>()
    private var sizeAssume = 0

    private val newBundler: FabricBundlePacketImpl
        get() {
            return _creator().also { bundler ->
                sizeAssume = 0
                subBundlers += bundler
            }
        }

    private var selectedBundler = newBundler

    override fun send(player: PlatformPlayer, onSuccess: Runnable) {
        if (isEmpty) {
            return
        }

        subBundlers.forEach {
            player.unwarp().connection.send(it.bundlePacket)
        }
    }

    override fun isEmpty(): Boolean = selectedBundler.isEmpty()

    override fun size(): Int = subBundlers.sumOf(FabricBundlePacketImpl::size)

    fun add(other: Packet<ClientGamePacketListener>) {
        val bundler = if (sizeAssume > threshold) {
            newBundler
        } else {
            selectedBundler
        }

        selectedBundler = bundler
        selectedBundler.add(other)

        sizeAssume += other.assumeSize()
    }
}

private val KEY: Key = Key.key(modId())
