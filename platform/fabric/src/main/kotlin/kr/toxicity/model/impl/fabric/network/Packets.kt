/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.network

import com.mojang.datafixers.util.Pair.of
import io.netty.buffer.Unpooled
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.mixin.ConnectionAccessor
import kr.toxicity.model.mixin.EntityAccessor
import kr.toxicity.model.mixin.ServerCommonPacketListenerImplAccessor
import kr.toxicity.model.mixin.SynchedEntityDataAccessor
import net.minecraft.network.Connection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.*
import java.util.stream.IntStream

val Connection.channel get() = (this as ConnectionAccessor).`bettermodel$getChannel`()

val ServerGamePacketListenerImpl.connection get() = (this as ServerCommonPacketListenerImplAccessor).`bettermodel$getConnection`()

val Player.hotbarSlot get() = inventory.selectedSlot + 36

fun EntityTrackerRegistry.mountPacket(
    entity: Entity,
    passengerIds: IntStream = entity.getUnregisteredPassengerIds()
): ClientboundSetPassengersPacket {
    return useByteBuf { buffer ->
        val displayIds = displays().mapToInt { display -> display.id() }
        val ids = IntStream.concat(displayIds, passengerIds)

        buffer.writeVarInt(entity.id)
        buffer.writeVarIntArray(ids.toArray())

        ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer)
    }
}

fun Entity.getUnregisteredPassengerIds(): IntStream {
    return passengers.stream()
        .filter { passenger ->
            EntityTrackerRegistry.registry(passenger.uuid) == null
        }
        .mapToInt { passenger ->
            passenger.id
        }
}

inline fun <T> useByteBuf(block: (FriendlyByteBuf) -> T): T {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    return try {
        block(buffer)
    } finally {
        buffer.release()
    }
}

inline fun SynchedEntityData.pack(
    clean: Boolean = false,
    itemFilter: (DataItem<*>) -> Boolean = { true },
    crossinline valueFilter: (DataValue<*>) -> Boolean = { true },
    crossinline required: (List<Pair<DataItem<*>, DataValue<*>>>) -> Boolean = { it.isNotEmpty() }
): List<DataValue<*>>? {
    return (this as SynchedEntityDataAccessor)
        .`bettermodel$getItemsById`()
        .mapNotNull {
            val item = it.takeIf(itemFilter)
                ?: return@mapNotNull null

            val value = item.value().takeIf(valueFilter)
                ?: return@mapNotNull null

            item to value
        }
        .takeIf(required)
        ?.map {
            if (clean) {
                it.first.isDirty = false
            }

            it.second
        }
}

fun ClientboundSetEntityDataPacket.toRegistryDataPacket(uuid: UUID, registry: EntityTrackerRegistry) = ClientboundSetEntityDataPacket(
    id, packedItems().map {
    if (it.id == EntityAccessor.`bettermodel$getDataSharedFlagsId`().id) DataValue(
        it.id,
        EntityDataSerializers.BYTE,
        registry.entityFlag(uuid, it.value() as Byte)
    ) else it
})

fun EntityTrackerRegistry.entityFlag(uuid: UUID, byte: Byte): Byte {
    var b = byte.toInt()
    val hideOption = hideOption(uuid)
    if (hideOption.fire()) b = b and 1.inv()
    if (hideOption.visibility()) b = b or (1 shl 5)
    if (hideOption.glowing()) b = b and (1 shl 6).inv()
    return b.toByte()
}

inline fun LivingEntity.toEquipmentPacket(mapper: (EquipmentSlot) -> ItemStack? = { if (hasItemInSlot(it)) getItemBySlot(it) else null }): ClientboundSetEquipmentPacket? {
    val equip = EquipmentSlot.entries.mapNotNull {
        mapper(it)?.let { item -> of(it, item) }
    }
    return if (equip.isNotEmpty()) ClientboundSetEquipmentPacket(id, equip) else null
}
fun LivingEntity.toEmptyEquipmentPacket() = toEquipmentPacket { ItemStack.EMPTY }

fun ClientboundContainerSetSlotPacket.isEquipment(player: Player): Boolean {
    return containerId == 0 &&
        (PLAYER_EQUIPMENT_SLOT.contains(slot) || slot == player.hotbarSlot)
}

fun eachEquipmentSlots(block: (Int) -> Unit) {
    PLAYER_EQUIPMENT_SLOT.forEach { slot ->
        block(slot)
    }
}

private val PLAYER_EQUIPMENT_SLOT = setOf(45, 5, 6, 7, 8)
