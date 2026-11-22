/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.nms.v1_21_R6

import kr.toxicity.model.api.nms.PacketBundler
import kr.toxicity.model.api.platform.PlatformPlayer
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Keyed
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import org.bukkit.craftbukkit.entity.CraftPlayer

private val KEY = Key.key("bettermodel")

internal fun bundlerOfNotNull(vararg packets: ClientPacket?) = PacketBundlerImpl(if (packets.isEmpty()) arrayListOf() else packets.filterNotNull().toMutableList())
internal fun bundlerOf(vararg packets: ClientPacket) = PacketBundlerImpl(if (packets.isEmpty()) arrayListOf() else packets.toMutableList())
internal fun bundlerOf(size: Int) = PacketBundlerImpl(ArrayList(size))

internal operator fun PacketBundler.plusAssign(other: ClientPacket) = (this as PacketBundlerImpl).add(other)

internal class PacketBundlerImpl(
    private val list: MutableList<ClientPacket>
) : Iterable<ClientPacket>, Keyed, PacketBundler {
    val bundlePacket = ClientboundBundlePacket(this)
    override fun send(player: PlatformPlayer, onSuccess: Runnable) {
        if (isEmpty) return
        val connection = (player.unwarp() as CraftPlayer).handle.connection
        connection.send(bundlePacket, PacketSendListener.thenRun(onSuccess))
    }
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun size(): Int = list.size
    override fun key(): Key = KEY
    override fun iterator(): MutableIterator<ClientPacket> = list.iterator()

    fun add(other: ClientPacket) {
        list += other
    }
}
